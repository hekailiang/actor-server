package com.secretapp.backend.api

import akka.actor._
import akka.testkit._
import com.secretapp.backend.util.ACL
import com.secretapp.backend.crypto.ec
import com.secretapp.backend.data.message._
import com.secretapp.backend.data.message.struct
import com.secretapp.backend.data.message.rpc._
import com.secretapp.backend.data.message.rpc.messaging._
import com.secretapp.backend.data.message.rpc.history._
import com.secretapp.backend.data.message.rpc.update._
import com.secretapp.backend.data.message.update._
import com.secretapp.backend.models
import com.secretapp.backend.data.transport._
import com.secretapp.backend.persist
import com.secretapp.backend.protocol.codecs.message.MessageBoxCodec
import com.secretapp.backend.services.rpc.RpcSpec
import com.websudos.util.testing._
import java.util.UUID
import scala.collection.immutable
import scala.concurrent.Await
import scala.concurrent.duration._
import scala.language.higherKinds
import scodec.bits._

class RpcMessagingSpec extends RpcSpec {
  def getState(implicit scope: TestScope): (ResponseSeq, Seq[UpdateBox]) = {
    implicit val TestScope(probe: TestProbe, destActor: ActorRef, s: SessionIdentifier, u: models.User) = scope

    RequestGetState() :~> <~:[ResponseSeq]
  }

  def getDifference(seq: Int, state: Option[UUID])(implicit scope: TestScope): ResponseGetDifference = {
    implicit val TestScope(probe: TestProbe, destActor: ActorRef, s: SessionIdentifier, u: models.User) = scope

    val rq = RequestGetDifference(seq, state)
    val messageId = getMessageId()
    val rpcRq = RpcRequestBox(Request(rq))
    val packageBlob = pack(0, u.authId, MessageBox(messageId, rpcRq))
    send(packageBlob)

    val msg = receiveOneWithAck

    msg
      .body.asInstanceOf[RpcResponseBox]
      .body.asInstanceOf[Ok]
      .body.asInstanceOf[ResponseGetDifference]
  }

  "RpcMessaging" should {
    "deliver unencrypted messages" in {
      val (scope1, scope2) = TestScope.pair(rand.nextInt, rand.nextInt)
      catchNewSession(scope1)
      catchNewSession(scope2)

      {
        implicit val scope = scope1

        RequestSendMessage(
          outPeer = struct.OutPeer.privat(scope2.user.uid, ACL.userAccessHash(scope.user.authId, scope2.user)),
          randomId = 1L,
          message = TextMessage("Yolo!")
        ) :~> <~:[ResponseSeqDate]

        val (diff, _) = RequestGetDifference(0, None) :~> <~:[ResponseGetDifference]

        diff.updates.length should beEqualTo(1)
        val upd = diff.updates.last.body.assertInstanceOf[MessageSent]
        upd.peer should_==(struct.Peer.privat(scope2.user.uid))
      }

      Thread.sleep(1000)

      {
        implicit val scope = scope2

        val (diff, _) = RequestGetDifference(0, None) :~> <~:[ResponseGetDifference]

        diff.updates.length should beEqualTo(1)
        val upd = diff.updates.last.body.assertInstanceOf[Message]
        upd.peer should_==(struct.Peer.privat(scope1.user.uid))
        upd.senderUid should_==(scope1.user.uid)
        upd.randomId should_==(1L)
        upd.message should_==(TextMessage("Yolo!"))
      }
    }

    "deliver unencrypted UpdateMessageReceived" in {
      val (scope1, scope2) = TestScope.pair(rand.nextInt, rand.nextInt)
      catchNewSession(scope1)
      catchNewSession(scope2)

      {
        implicit val scope = scope1

        RequestSendMessage(
          outPeer = struct.OutPeer.privat(scope2.user.uid, ACL.userAccessHash(scope.user.authId, scope2.user)),
          randomId = 1L,
          message = TextMessage("Yolo!")
        ) :~> <~:[ResponseSeqDate]
      }

      Thread.sleep(1000)

      val date = System.currentTimeMillis()

      {
        implicit val scope = scope2

        RequestMessageReceived(
          outPeer = struct.OutPeer.privat(scope1.user.uid, ACL.userAccessHash(scope.user.authId, scope1.user)),
          date = date
        ) :~> <~:[ResponseVoid]
      }

      {
        implicit val scope = scope1

        val (diff, _) = RequestGetDifference(0, None) :~> <~:[ResponseGetDifference]

        diff.updates.length should beEqualTo(2)
        val upd = diff.updates.last.body.assertInstanceOf[MessageReceived]
        upd.peer should_==(struct.Peer.privat(scope2.user.uid))
        upd.startDate should_==(date)
      }
    }

    "deliver unencrypted UpdateMessageRead" in {
      val (scope1, scope2) = TestScope.pair(rand.nextInt, rand.nextInt)
      catchNewSession(scope1)
      catchNewSession(scope2)

      {
        implicit val scope = scope1

        RequestSendMessage(
          outPeer = struct.OutPeer.privat(scope2.user.uid, ACL.userAccessHash(scope.user.authId, scope2.user)),
          randomId = 1L,
          message = TextMessage("Yolo!")
        ) :~> <~:[ResponseSeqDate]
      }

      Thread.sleep(1000)

      val date = System.currentTimeMillis()

      {
        implicit val scope = scope2

        RequestMessageRead(
          outPeer = struct.OutPeer.privat(scope1.user.uid, ACL.userAccessHash(scope.user.authId, scope1.user)),
          date = date
        ) :~> <~:[ResponseVoid]

        val (diff, _) = RequestGetDifference(0, None) :~> <~:[ResponseGetDifference]

        diff.updates.length should beEqualTo(2)
        val upd = diff.updates.last.body.assertInstanceOf[MessageReadByMe]
        upd.peer should_==(struct.Peer.privat(scope1.user.uid))
        upd.date should_==(date)
      }

      {
        implicit val scope = scope1

        val (diff, _) = RequestGetDifference(0, None) :~> <~:[ResponseGetDifference]

        diff.updates.length should beEqualTo(2)
        val upd = diff.updates.last.body.assertInstanceOf[MessageRead]
        upd.peer should_==(struct.Peer.privat(scope2.user.uid))
        upd.date should_==(date)
      }
    }

    "reply to SendMessage and push to sequence" in {
      //implicit val (probe, apiActor) = probeAndActor()
      //implicit val sessionId = SessionIdentifier()
      implicit val scope = TestScope()

      val publicKey = hex"ac1d".bits
      val publicKeyHash = ec.PublicKey.keyHash(publicKey)
      val name = "Timothy Klim"
      val pkHash = ec.PublicKey.keyHash(publicKey)
      val user = models.User(userId, mockAuthId, pkHash, publicKey, defaultPhoneNumber, userSalt, name, "RU", models.NoSex, keyHashes = immutable.Set(pkHash))
      val accessHash = ACL.userAccessHash(scope.user.authId, userId, userSalt)
      authUser(user, defaultPhoneNumber)

      // insert second user
      val sndPublicKey = hex"ac1d3000".bits
      val sndUID = 3000
      val sndPkHash = ec.PublicKey.keyHash(sndPublicKey)
      val secondUser = models.User(sndUID, 333L, sndPkHash, sndPublicKey, defaultPhoneNumber, userSalt, name, "RU", models.NoSex, keyHashes = immutable.Set(sndPkHash))
      persist.User.insertEntityWithChildren(secondUser, models.AvatarData.empty).sync()

      /**
        * This sleep is needed to let sharding things to initialize
        * We catch an exception sometimes:
        * java.lang.IllegalArgumentException: requirement failed: Region Actor[akka.tcp://api@127.0.0.1:46807/user/sharding/Typing#1848681712] not registered: State(Map(),Map(Actor[akka.tcp://api@127.0.0.1:51745/user/sharding/Typing#1056784136] -> Vector()),Set())
        */
      Thread.sleep(1000)

      catchNewSession(scope)

      // get initial state
      val (initialState, _) = getState

      val rq = RequestSendEncryptedMessage(
        struct.OutPeer.privat(secondUser.uid, ACL.userAccessHash(scope.user.authId, secondUser)),
        randomId = 555L,
        encryptedMessage = BitVector(1, 2, 3),
        keys = immutable.Seq(
          EncryptedAESKey(
            secondUser.publicKeyHash, BitVector(1, 0, 1, 0)
          )
        ),
        ownKeys = immutable.Seq(
          EncryptedAESKey(
            scope.user.publicKeyHash, BitVector(1, 0, 1, 0)
          )
        )
      )

      val (resp, _) = rq :~> <~:[ResponseSeqDate]
      resp.seq should beEqualTo(initialState.seq + 2)

      val (state, _) = getState
      state.seq must equalTo(initialState.seq + 2)
      getDifference(initialState.seq, initialState.state).updates.length must equalTo(2)

      {
        // same randomId
        val (resp, _) = rq :~> <~:[ResponseSeqDate]
        resp.seq should beEqualTo(initialState.seq + 2)
      }

      Thread.sleep(1000)

      getState._1.seq must equalTo(initialState.seq + 2)
    }

    "send UpdateMessageReceived on RequestMessageReceived" in {
      val (scope1, scope2) = TestScope.pair(3, 4)
      catchNewSession(scope1)
      catchNewSession(scope2)

      {
        implicit val scope = scope1

        val rq = RequestSendEncryptedMessage(
          outPeer = struct.OutPeer.privat(scope2.user.uid, ACL.userAccessHash(scope.user.authId, scope2.user)),
          randomId = 555L,
          encryptedMessage = BitVector(1, 2, 3),
          keys = immutable.Seq(
            EncryptedAESKey(
              scope2.user.publicKeyHash, BitVector(1, 0, 1, 0)
            )
          ),
          ownKeys = immutable.Seq(
            EncryptedAESKey(
              scope1.user.publicKeyHash, BitVector(1, 0, 1, 0)
            )
          )
        )

        rq :~> <~:[ResponseSeqDate]

        // subscribe to updates
        getState(scope)
      }

      {
        implicit val scope = scope2

        RequestEncryptedReceived(struct.OutPeer.privat(scope1.user.uid, ACL.userAccessHash(scope.user.authId, scope1.user)), 555L) :~> <~:[ResponseVoid]
      }

      {
        implicit val scope = scope1

        val p = protoReceiveN(1)(scope.probe, scope.apiActor)
        val updBox = MessageBoxCodec.decodeValidValue(p.head.messageBoxBytes).body.asInstanceOf[UpdateBox]
        val update = updBox.body.asInstanceOf[SeqUpdate]
        update.body should beAnInstanceOf[EncryptedReceived]
      }
    }

    "send UpdateMessageRead on RequestMessageRead" in {
      val (scope1, scope2) = TestScope.pair(5, 6)
      catchNewSession(scope1)
      catchNewSession(scope2)

      {
        implicit val scope = scope1

        val rq = RequestSendEncryptedMessage(
          outPeer = struct.OutPeer.privat(scope2.user.uid, ACL.userAccessHash(scope.user.authId, scope2.user)),
          randomId = 555L,
          encryptedMessage = BitVector(1, 2, 3),
          keys = immutable.Seq(
            EncryptedAESKey(
              scope2.user.publicKeyHash, BitVector(1, 0, 1, 0)
            )
          ),
          ownKeys = immutable.Seq(
            EncryptedAESKey(
              scope1.user.publicKeyHash, BitVector(1, 0, 1, 0)
            )
          )
        )

        rq :~> <~:[ResponseSeqDate]

        // subscribe to updates
        getState(scope)
      }

      Thread.sleep(500)

      {
        implicit val scope = scope2

        RequestEncryptedRead(struct.OutPeer.privat(scope1.user.uid, ACL.userAccessHash(scope.user.authId, scope1.user)), 555L) :~> <~:[ResponseVoid]
      }

      {
        implicit val scope = scope1

        val p = protoReceiveN(1)(scope.probe, scope.apiActor)
        val updBox = MessageBoxCodec.decodeValidValue(p.head.messageBoxBytes).body.asInstanceOf[UpdateBox]
        val update = updBox.body.asInstanceOf[SeqUpdate]
        update.body should beAnInstanceOf[EncryptedRead]

        val (diff, _) = RequestGetDifference(0, None) :~> <~:[ResponseGetDifference]
        diff.updates.last.body should beAnInstanceOf[EncryptedRead]
      }
    }

    "check provided keys" in {
      val (scope1, scope2) = TestScope.pair(rand.nextInt(), rand.nextInt())
      val scope2_2 = TestScope(scope2.user.uid, scope2.user.phoneNumber)

      catchNewSession(scope1)
      catchNewSession(scope2)
      catchNewSession(scope2_2)

      Await.result(persist.UserPublicKey.setDeleted(scope2.user.uid, scope2.user.publicKeyHash), DurationInt(3).seconds)

      {
        implicit val scope = scope1

        val rq = RequestSendEncryptedMessage(
          outPeer = struct.OutPeer.privat(scope2.user.uid, ACL.userAccessHash(scope.user.authId, scope2.user)),
          randomId = 555L,
          encryptedMessage = BitVector(1, 2, 3),
          keys = immutable.Seq(
            EncryptedAESKey(
              scope2.user.publicKeyHash, BitVector(1, 0, 1, 0)
            )
          ),
          ownKeys = immutable.Seq(
            EncryptedAESKey(
              scope1.user.publicKeyHash, BitVector(1, 0, 1, 0)
            ),
            EncryptedAESKey(
              111L, BitVector(1, 0, 1, 0)
            )
          )
        )

        val error = rq :~> <~:(400, "WRONG_KEYS")
        error.data.get should equalTo(struct.WrongKeysErrorData(
          newKeys = Seq(struct.UserKey(scope2_2.user.uid, scope2_2.user.publicKeyHash)),
          removedKeys = Seq(struct.UserKey(scope2.user.uid, scope2.user.publicKeyHash)),
          invalidKeys = Seq(struct.UserKey(scope1.user.uid, 111L))
        ))
      }

      {
        implicit val scope = scope1

        val rq = RequestSendEncryptedMessage(
          outPeer = struct.OutPeer.privat(scope2.user.uid, ACL.userAccessHash(scope.user.authId, scope2.user)),
          randomId = 555L,
          encryptedMessage = BitVector(1, 2, 3),
          keys = immutable.Seq(
            EncryptedAESKey(
              scope2.user.publicKeyHash, BitVector(1, 0, 1, 0)
            )
          ),
          ownKeys = immutable.Seq(
            EncryptedAESKey(
              111L, BitVector(1, 0, 1, 0)
            )
          )
        )

        val error = rq :~> <~:(400, "WRONG_KEYS")
        error.data.get should equalTo(struct.WrongKeysErrorData(
          newKeys = Seq(struct.UserKey(scope2_2.user.uid, scope2_2.user.publicKeyHash)),
          removedKeys = Seq(struct.UserKey(scope2.user.uid, scope2.user.publicKeyHash)),
          invalidKeys = Seq(struct.UserKey(scope1.user.uid, 111L))
        ))
      }
    }

    "load history of unencrypted messages" in {
      val (scope1, scope2) = TestScope.pair(rand.nextInt, rand.nextInt)
      catchNewSession(scope1)
      catchNewSession(scope2)

      {
        implicit val scope = scope1

        RequestSendMessage(
          outPeer = struct.OutPeer.privat(scope2.user.uid, ACL.userAccessHash(scope.user.authId, scope2.user)),
          randomId = 1L,
          message = TextMessage("Yolo from user1!")
        ) :~> <~:[ResponseSeqDate]
      }

      {
        implicit val scope = scope2

        RequestSendMessage(
          outPeer = struct.OutPeer.privat(scope1.user.uid, ACL.userAccessHash(scope.user.authId, scope1.user)),
          randomId = 1L,
          message = TextMessage("Yolo from user2!")
        ) :~> <~:[ResponseSeqDate]
      }

      Thread.sleep(1000)

      {
        implicit val scope = scope1

        val (resp, _) = RequestLoadHistory(
          outPeer = struct.OutPeer.privat(scope2.user.uid, ACL.userAccessHash(scope.user.authId, scope2.user)),
          startDate = 0,
          limit = 2
        ) :~> <~:[ResponseLoadHistory]

        resp.users.length should_== 1
        resp.history.length should_== 2
      }
    }

    "clear chat and send updates" in {
      val (scope1, scope2) = TestScope.pair(rand.nextInt, rand.nextInt)
      catchNewSession(scope1)

      {
        implicit val scope = scope1

        val outPeer = struct.OutPeer.privat(scope2.user.uid, ACL.userAccessHash(scope.user.authId, scope2.user))

        RequestSendMessage(
          outPeer = outPeer,
          randomId = 1L,
          message = TextMessage("Yolo from user1 to user2! #1")
        ) :~> <~:[ResponseSeqDate]

        RequestClearChat(outPeer) :~> <~:[ResponseSeq]

        val (diff, _) = RequestGetDifference(0, None) :~> <~:[ResponseGetDifference]

        diff.updates.length should beEqualTo(2)
        val upd = diff.updates.last.body.assertInstanceOf[ChatClear]

        Thread.sleep(1000)

        persist.HistoryMessage.fetchByPeer(scope.user.uid, outPeer.asPeer, 0, 10).sync().length should_== 0
      }

    }

    "delete chat and send updates" in {
      val (scope1, scope2) = TestScope.pair(rand.nextInt, rand.nextInt)
      catchNewSession(scope1)

      {
        implicit val scope = scope1

        val outPeer = struct.OutPeer.privat(scope2.user.uid, ACL.userAccessHash(scope.user.authId, scope2.user))

        RequestSendMessage(
          outPeer = outPeer,
          randomId = 1L,
          message = TextMessage("Yolo from user1 to user2! #1")
        ) :~> <~:[ResponseSeqDate]

        RequestDeleteChat(outPeer) :~> <~:[ResponseSeq]

        val (diff, _) = RequestGetDifference(0, None) :~> <~:[ResponseGetDifference]

        diff.updates.length should beEqualTo(2)
        val upd = diff.updates.last.body.assertInstanceOf[ChatDelete]

        Thread.sleep(1000)

        persist.HistoryMessage.fetchByPeer(scope.user.uid, outPeer.asPeer, 0, 10).sync().length should_== 0
        persist.Dialog.fetchDialogs(scope.user.uid, 0, 0).sync().length should_== 0
      }

    }

    "load dialogs in proper order" in {
      val (scope1, scope2) = TestScope.pair(rand.nextInt, rand.nextInt)
      val scope3 = TestScope(rand.nextInt)
      catchNewSession(scope1)
      catchNewSession(scope2)
      catchNewSession(scope3)

      {
        implicit val scope = scope1

        RequestSendMessage(
          outPeer = struct.OutPeer.privat(scope2.user.uid, ACL.userAccessHash(scope.user.authId, scope2.user)),
          randomId = 1L,
          message = TextMessage("Yolo from user1 to user2! #1")
        ) :~> <~:[ResponseSeqDate]

        Thread.sleep(1000)

        RequestSendMessage(
          outPeer = struct.OutPeer.privat(scope3.user.uid, ACL.userAccessHash(scope.user.authId, scope3.user)),
          randomId = 2L,
          message = TextMessage("Yolo from user1 to user3! #1")
        ) :~> <~:[ResponseSeqDate]

        Thread.sleep(2000)

        {
          val (resp, _) = RequestLoadDialogs(
            startDate = 0,
            limit = 10
          ) :~> <~:[ResponseLoadDialogs]

          resp.dialogs.length should_== 2
          resp.dialogs.head.message should_== TextMessage("Yolo from user1 to user3! #1")
          resp.dialogs.head.peer.id should_== scope3.user.uid
        }

        RequestSendMessage(
          outPeer = struct.OutPeer.privat(scope2.user.uid, ACL.userAccessHash(scope.user.authId, scope2.user)),
          randomId = 3L,
          message = TextMessage("Yolo from user1 to user2! #2")
        ) :~> <~:[ResponseSeqDate]

        Thread.sleep(2000)

        {
          val (resp, _) = RequestLoadDialogs(
            startDate = 0,
            limit = 10
          ) :~> <~:[ResponseLoadDialogs]

          resp.dialogs.length should_== 2
          resp.dialogs.head.message should_== TextMessage("Yolo from user1 to user2! #2")
          resp.dialogs.head.peer.id should_== scope2.user.uid
          resp.dialogs.head.unreadCount should_== 2
        }
      }
    }
  }
}
