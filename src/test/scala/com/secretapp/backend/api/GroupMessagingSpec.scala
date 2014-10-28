package com.secretapp.backend.api

import akka.actor._
import akka.io.Tcp._
import akka.testkit._
import com.websudos.util.testing.AsyncAssertionsHelper._
import com.secretapp.backend.crypto.ec
import com.secretapp.backend.data._
import com.secretapp.backend.data.message._
import com.secretapp.backend.data.message.rpc._
import com.secretapp.backend.data.message.rpc.messaging._
import com.secretapp.backend.data.message.rpc.{ update => updateProto }
import com.secretapp.backend.data.message.struct.UserId
import com.secretapp.backend.data.message.update._
import com.secretapp.backend.data.models._
import com.secretapp.backend.data.transport._
import com.secretapp.backend.persist._
import com.secretapp.backend.protocol.codecs.message.MessageBoxCodec
import com.secretapp.backend.services.common.RandomService
import com.secretapp.backend.services.GeneratorService
import com.secretapp.backend.services.rpc.RpcSpec
import java.util.UUID
import org.specs2.mutable.{ ActorLikeSpecification, ActorServiceHelpers }
import scala.collection.immutable
import scala.language.higherKinds
import scala.util.Random
import scalaz.Scalaz._
import scodec.bits._
import scodec.codecs.uuid

class GroupMessagingSpec extends RpcSpec {
  import system.dispatcher

  "GroupMessaging" should {
    "send updates on name change" in {
      val (scope1, scope2) = TestScope.pair()
      catchNewSession(scope1)
      catchNewSession(scope2)

      {
        implicit val scope = scope1
        val rqCreateGroup = RequestCreateGroup(
          randomId = 1L,
          title = "Group 3000",
          keyHash = BitVector(1, 1, 1),
          publicKey = BitVector(1, 0, 1, 0),
          broadcast = EncryptedRSABroadcast(
            encryptedMessage = BitVector(1, 2, 3),
            keys = immutable.Seq(
              EncryptedUserAESKeys(
                userId = scope2.user.uid,
                accessHash = User.getAccessHash(scope1.user.authId, scope2.user.uid, scope2.user.accessSalt),
                keys = immutable.Seq(
                  EncryptedAESKey(
                    keyHash = scope2.user.publicKeyHash,
                    aesEncryptedKey = BitVector(2, 0, 2, 0)
                  )
                )
              )
            ),
            ownKeys = immutable.Seq(
              EncryptedAESKey(
                keyHash = scope.user.publicKeyHash,
                aesEncryptedKey = BitVector(2, 0, 2, 0)
              )
            )
          )
        )
        val (resp, _) = rqCreateGroup :~> <~:[ResponseCreateGroup]

        Thread.sleep(500)

        RequestEditGroupTitle(
          groupId = resp.groupId,
          accessHash = resp.accessHash,
          title = "Title 3000"
        ) :~> <~:[updateProto.ResponseSeq]

        Thread.sleep(500)

        val (diff, _) = updateProto.RequestGetDifference(0, None) :~> <~:[updateProto.Difference]

        diff.updates.length should beEqualTo(2)
        diff.updates.last.body.assertInstanceOf[GroupTitleChanged]
      }

      {
        implicit val scope = scope2

        val (diff, _) = updateProto.RequestGetDifference(0, None) :~> <~:[updateProto.Difference]

        diff.updates.length should beEqualTo(2)
        diff.updates.last.body.assertInstanceOf[GroupTitleChanged]
      }
    }

    "send invites on creation and send/receive messages" in {
      val (scope1, scope2) = TestScope.pair()
      catchNewSession(scope1)
      catchNewSession(scope2)
      val scope11 = TestScope(scope1.user.uid, scope1.user.phoneNumber)
      catchNewSession(scope11)

      {
        implicit val scope = scope1
        val rqCreateGroup = RequestCreateGroup(
          randomId = 1L,
          title = "Group 3000",
          keyHash = BitVector(1, 1, 1),
          publicKey = BitVector(1, 0, 1, 0),
          broadcast = EncryptedRSABroadcast(
            encryptedMessage = BitVector(1, 2, 3),
            keys = immutable.Seq(
              EncryptedUserAESKeys(
                userId = scope2.user.uid,
                accessHash = User.getAccessHash(scope1.user.authId, scope2.user.uid, scope2.user.accessSalt),
                keys = immutable.Seq(
                  EncryptedAESKey(
                    keyHash = scope2.user.publicKeyHash,
                    aesEncryptedKey = BitVector(2, 0, 2, 0)
                  )
                )
              )
            ),
            ownKeys = immutable.Seq(
              EncryptedAESKey(
                keyHash = scope.user.publicKeyHash,
                aesEncryptedKey = BitVector(2, 0, 2, 0)
              ),
              EncryptedAESKey(
                keyHash = scope11.user.publicKeyHash,
                aesEncryptedKey = BitVector(2, 0, 2, 0)
              )
            )
          )
        )
        val (resp, _) = rqCreateGroup :~> <~:[ResponseCreateGroup]

        Thread.sleep(500)

        val rqSendMessage = RequestSendGroupMessage(
          groupId = resp.groupId,
          accessHash = resp.accessHash,
          randomId = 666L,
          message = EncryptedAESMessage(
            keyHash = BitVector(1, 1, 1),
            encryptedMessage = BitVector(1, 2, 3)
          )
        )

        rqSendMessage :~> <~:[updateProto.ResponseSeq]

        Thread.sleep(500)

        val (diff, _) = updateProto.RequestGetDifference(0, None) :~> <~:[updateProto.Difference]

        diff.updates.length should beEqualTo(2)
        diff.updates.head.body.assertInstanceOf[GroupCreated]
      }

      {
        implicit val scope = scope11

        val (diff, _) = updateProto.RequestGetDifference(0, None) :~> <~:[updateProto.Difference]

        diff.updates.length should beEqualTo(2)
        diff.updates(0).body.assertInstanceOf[GroupCreated]
      }

      {
        implicit val scope = scope2

        val (diff, _) = updateProto.RequestGetDifference(0, None) :~> <~:[updateProto.Difference]

        val invite = diff.updates.head.body.assertInstanceOf[GroupInvite]

        invite.users.toSet should beEqualTo(Set(
          UserId(scope1.user.uid, scope1.user.accessHash(scope.user.authId)),
          UserId(scope2.user.uid, scope2.user.accessHash(scope.user.authId))
        ))

        diff.updates(1).body.assertInstanceOf[GroupMessage]
      }
    }

    "send invites on RequestInviteUser" in {
      val (scope1, scope2) = TestScope.pair(3, 4)
      catchNewSession(scope1)
      catchNewSession(scope2)

      {
        implicit val scope = scope1

        val groupKeyHash = BitVector(1, 1, 1)

        val rqCreateGroup = RequestCreateGroup(
          randomId = 1L,
          title = "Group 3000",
          keyHash = groupKeyHash,
          publicKey = BitVector(1, 0, 1, 0),
          broadcast = EncryptedRSABroadcast(
            encryptedMessage = BitVector(1, 2, 3),
            keys = immutable.Seq.empty,
            ownKeys = immutable.Seq.empty
          )
        )
        val (resp, _) = rqCreateGroup :~> <~:[ResponseCreateGroup]

        Thread.sleep(1000)

        val rqInviteUser = RequestInviteUsers(
          groupId = resp.groupId,
          accessHash = resp.accessHash,
          randomId = 666L,
          groupKeyHash = groupKeyHash,
          broadcast = EncryptedRSABroadcast(
            encryptedMessage = BitVector(1, 2, 3),
            keys = immutable.Seq(
              EncryptedUserAESKeys(
                userId = scope2.user.uid,
                accessHash = scope2.user.accessHash(scope.user.authId),
                keys = immutable.Seq(
                  EncryptedAESKey(
                    keyHash = scope2.user.publicKeyHash,
                    aesEncryptedKey = BitVector(2, 0, 2, 0)
                  )
                )
              )
            ),
            ownKeys = immutable.Seq.empty
          )
        )

        rqInviteUser :~> <~:[updateProto.ResponseSeq]

        val (diff, _) = updateProto.RequestGetDifference(0, None) :~> <~:[updateProto.Difference]
        diff.updates.last.body.assertInstanceOf[GroupUserAdded]
      }

      {
        implicit val scope = scope2

        val (diff, _) = updateProto.RequestGetDifference(0, None) :~> <~:[updateProto.Difference]
        val update = diff.updates.head.body.assertInstanceOf[GroupInvite]
        update.users should beEqualTo(Seq(UserId(scope1.user.uid, scope1.user.accessHash(scope2.user.authId))))
      }
    }

    "send GroupUserLeave on user leave" in {
      val (scope1, scope2) = TestScope.pair(5, 6)
      catchNewSession(scope1)
      catchNewSession(scope2)
      val scope11 = TestScope(scope1.user.uid, scope1.user.phoneNumber)
      catchNewSession(scope11)

      {
        implicit val scope = scope1

        val rqCreateGroup = RequestCreateGroup(
          randomId = 1L,
          title = "Group 3000",
          keyHash = BitVector(1, 1, 1),
          publicKey = BitVector(1, 0, 1, 0),
          broadcast = EncryptedRSABroadcast(
            encryptedMessage = BitVector(1, 2, 3),
            keys = immutable.Seq(
              EncryptedUserAESKeys(
                userId = scope2.user.uid,
                accessHash = User.getAccessHash(scope1.user.authId, scope2.user.uid, scope2.user.accessSalt),
                keys = immutable.Seq(
                  EncryptedAESKey(
                    keyHash = scope2.user.publicKeyHash,
                    aesEncryptedKey = BitVector(2, 0, 2, 0)
                  )
                )
              )
            ),
            ownKeys = immutable.Seq.empty
          )
        )
        val (resp, _) = rqCreateGroup :~> <~:[ResponseCreateGroup]

        Thread.sleep(1000)

        RequestLeaveGroup(
          groupId = resp.groupId,
          accessHash = resp.accessHash
        ) :~> <~:[updateProto.ResponseSeq]
      }

      Thread.sleep(1000)

      {
        implicit val scope = scope11

        val (diff, _) = updateProto.RequestGetDifference(0, None) :~> <~:[updateProto.Difference]
        diff.updates.last.body.assertInstanceOf[GroupUserLeave]
      }

      {
        implicit val scope = scope2

        val (diff, _) = updateProto.RequestGetDifference(0, None) :~> <~:[updateProto.Difference]
        diff.updates.head.body.assertInstanceOf[GroupInvite]
        diff.updates(1).body.assertInstanceOf[GroupUserLeave]
      }
    }

    "not allow to send messages to group if user is not a member of this group" in {
      val (scope1, scope2) = TestScope.pair()
      catchNewSession(scope1)
      catchNewSession(scope2)

      val group = {
        implicit val scope = scope1

        val rqCreateGroup = RequestCreateGroup(
          randomId = 1L,
          title = "Group 3000",
          keyHash = BitVector(1, 1, 1),
          publicKey = BitVector(1, 0, 1, 0),
          broadcast = EncryptedRSABroadcast(
            encryptedMessage = BitVector(1, 2, 3),
            keys = immutable.Seq.empty,
            ownKeys = immutable.Seq.empty
          )
        )
        val (resp, _) = rqCreateGroup :~> <~:[ResponseCreateGroup]

        resp
      }

      {
        implicit val scope = scope2

        val rqSendMessage = RequestSendGroupMessage(
          groupId = group.groupId,
          accessHash = group.accessHash,
          randomId = 666L,
          EncryptedAESMessage(
            keyHash = BitVector(1, 1, 1),
            encryptedMessage = BitVector(1, 2, 3)
          )
        )

        rqSendMessage :~> <~:(403, "NO_PERMISSION")
      }
    }

    "not send GroupUserAdded after inviting user who is already a group member" in {
      val (scope1, scope2) = TestScope.pair(5, 6)
      catchNewSession(scope1)
      catchNewSession(scope2)
      val scope22 = TestScope(scope2.user.uid, scope2.user.phoneNumber)

      {
        implicit val scope = scope1

        val groupKeyHash = BitVector(1, 1, 1)

        val rqCreateGroup = RequestCreateGroup(
          randomId = 1L,
          title = "Group 3000",
          keyHash = groupKeyHash,
          publicKey = BitVector(1, 0, 1, 0),
          broadcast = EncryptedRSABroadcast(
            encryptedMessage = BitVector(1, 2, 3),
            keys = immutable.Seq.empty,
            ownKeys = immutable.Seq.empty
          )
        )
        val (resp, _) = rqCreateGroup :~> <~:[ResponseCreateGroup]

        Thread.sleep(1000)

        {
          val rqInviteUser = RequestInviteUsers(
            groupId = resp.groupId,
            accessHash = resp.accessHash,
            randomId = 666L,
            groupKeyHash = groupKeyHash,
            broadcast = EncryptedRSABroadcast(
              encryptedMessage = BitVector(1, 2, 3),
              keys = immutable.Seq(
                EncryptedUserAESKeys(
                  userId = scope2.user.uid,
                  accessHash = scope2.user.accessHash(scope.user.authId),
                  keys = immutable.Seq(
                    EncryptedAESKey(
                      keyHash = scope2.user.publicKeyHash,
                      aesEncryptedKey = BitVector(2, 0, 2, 0)
                    )
                  )
                )
              ),
              ownKeys = immutable.Seq.empty
            )
          )

          rqInviteUser :~> <~:[updateProto.ResponseSeq]
        }

        val (diff1, _) = updateProto.RequestGetDifference(0, None) :~> <~:[updateProto.Difference]
        diff1.updates.length should beEqualTo(1)
        diff1.updates.last.body.assertInstanceOf[GroupUserAdded]

        {
          val rqInviteUser = RequestInviteUsers(
            groupId = resp.groupId,
            accessHash = resp.accessHash,
            randomId = 666L,
            groupKeyHash = groupKeyHash,
            broadcast = EncryptedRSABroadcast(
              encryptedMessage = BitVector(1, 2, 3),
              keys = immutable.Seq(
                EncryptedUserAESKeys(
                  userId = scope22.user.uid,
                  accessHash = scope22.user.accessHash(scope.user.authId),
                  keys = immutable.Seq(
                    EncryptedAESKey(
                      keyHash = scope22.user.publicKeyHash,
                      aesEncryptedKey = BitVector(2, 0, 2, 0)
                    )
                  )
                )
              ),
              ownKeys = immutable.Seq.empty
            )
          )

          rqInviteUser :~> <~:[updateProto.ResponseSeq]
        }

        val (diff2, _) = updateProto.RequestGetDifference(0, None) :~> <~:[updateProto.Difference]
        diff1.updates.length should beEqualTo(1)
        diff2.updates.last.body.assertInstanceOf[GroupUserAdded]
      }
    }
  }
}
