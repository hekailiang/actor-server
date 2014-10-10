package com.secretapp.backend.api

import akka.actor._
import akka.io.Tcp._
import akka.testkit._
import com.newzly.util.testing.AsyncAssertionsHelper._
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
import com.secretapp.backend.data.types._
import com.secretapp.backend.persist._
import com.secretapp.backend.protocol.codecs.message.MessageBoxCodec
import com.secretapp.backend.services.common.RandomService
import com.secretapp.backend.services.GeneratorService
import com.secretapp.backend.services.rpc.RpcSpec
import java.util.UUID
import org.scalamock.specs2.MockFactory
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
    "send invites on creation and send/receive messages" in {
      val (scope1, scope2) = TestScope.pair()
      val scope11 = TestScope(scope1.user.uid, scope1.user.phoneNumber)

      {
        implicit val scope = scope1
        val rqCreateChat = RequestCreateChat(
          randomId = 1L,
          title = "Groupchat 3000",
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
        val (resp, _) = rqCreateChat :~> <~:[ResponseCreateChat]

        Thread.sleep(500)

        val rqSendMessage = RequestSendGroupMessage(
          chatId = resp.chatId,
          accessHash = resp.accessHash,
          randomId = 666L,
          message = EncryptedAESMessage(
            keyHash = BitVector(1, 1, 1),
            encryptedMessage = BitVector(1, 2, 3)
          )
        )

        rqSendMessage :~> <~:[updateProto.ResponseSeq]

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

      {
        implicit val scope = scope1

        val chatKeyHash = BitVector(1, 1, 1)

        val rqCreateChat = RequestCreateChat(
          randomId = 1L,
          title = "Groupchat 3000",
          keyHash = chatKeyHash,
          publicKey = BitVector(1, 0, 1, 0),
          broadcast = EncryptedRSABroadcast(
            encryptedMessage = BitVector(1, 2, 3),
            keys = immutable.Seq.empty,
            ownKeys = immutable.Seq.empty
          )
        )
        val (resp, _) = rqCreateChat :~> <~:[ResponseCreateChat]

        Thread.sleep(1000)

        val rqInviteUser = RequestInviteUsers(
          chatId = resp.chatId,
          accessHash = resp.accessHash,
          randomId = 666L,
          chatKeyHash = chatKeyHash,
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
      val scope11 = TestScope(scope1.user.uid, scope1.user.phoneNumber)

      {
        implicit val scope = scope1

        val rqCreateChat = RequestCreateChat(
          randomId = 1L,
          title = "Groupchat 3000",
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
        val (resp, _) = rqCreateChat :~> <~:[ResponseCreateChat]

        Thread.sleep(1000)

        RequestLeaveChat(
          chatId = resp.chatId,
          accessHash = resp.accessHash
        ) :~> <~:[updateProto.ResponseSeq]
      }

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

      val chat = {
        implicit val scope = scope1

        val rqCreateChat = RequestCreateChat(
          randomId = 1L,
          title = "Groupchat 3000",
          keyHash = BitVector(1, 1, 1),
          publicKey = BitVector(1, 0, 1, 0),
          broadcast = EncryptedRSABroadcast(
            encryptedMessage = BitVector(1, 2, 3),
            keys = immutable.Seq.empty,
            ownKeys = immutable.Seq.empty
          )
        )
        val (resp, _) = rqCreateChat :~> <~:[ResponseCreateChat]

        resp
      }

      {
        implicit val scope = scope2

        val rqSendMessage = RequestSendGroupMessage(
          chatId = chat.chatId,
          accessHash = chat.accessHash,
          randomId = 666L,
          EncryptedAESMessage(
            keyHash = BitVector(1, 1, 1),
            encryptedMessage = BitVector(1, 2, 3)
          )
        )

        rqSendMessage :~> <~:(403, "NO_PERMISSION")
      }
    }
  }
}
