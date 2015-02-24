package com.secretapp.backend.services.rpc.updates

import com.secretapp.backend.data.message.UpdateBox
import com.secretapp.backend.data.message.rpc.ResponseVoid
import com.secretapp.backend.data.message.rpc.messaging._
import com.secretapp.backend.data.message.rpc.presence.RequestSetOnline
import com.secretapp.backend.data.message.rpc.update._
import com.secretapp.backend.data.message.struct
import com.secretapp.backend.data.message.update
import com.secretapp.backend.protocol.codecs.message.MessageBoxCodec
import com.secretapp.backend.services.rpc.RpcSpec
import com.secretapp.backend.util.ACL
import scala.collection.immutable
import scala.concurrent.duration._
import scodec.bits._

class UpdatesServiceSpec extends RpcSpec {
  import system.dispatcher

  "updates service" should {
    "not subscribe to updates twice" in new sqlDb {
      val (scope1, scope2) = TestScope.pair(3, 4)
      catchNewSession(scope1)
      catchNewSession(scope2)

      {
        implicit val scope = scope1

        val (state, _) = RequestGetState() :~> <~:[ResponseSeq]
        state.state must equalTo(None)
        RequestGetState() :~> <~:[ResponseSeq]
      }

      {
        implicit val scope = scope2
        val (state, _) = RequestGetState() :~> <~:[ResponseSeq]
        state.state must equalTo(None)

        val rq = RequestSendEncryptedMessage(
          outPeer = struct.OutPeer.privat(scope1.user.uid, ACL.userAccessHash(scope.user.authId, scope1.user)),
          randomId = 555L,
          encryptedMessage = BitVector(1, 2, 3),
          keys = immutable.Seq(
            EncryptedAESKey(
              scope1.user.publicKeyHash, BitVector(1, 0, 1, 0)
            )
          ),
          ownKeys = immutable.Seq(
            EncryptedAESKey(
              scope2.user.publicKeyHash, BitVector(1, 0, 1, 0)
            )
          )
        )

        rq :~> <~:[ResponseSeqDate]
      }

      {
        implicit val scope = scope1

        // Update
        protoReceiveN(1)(scope.probe, scope.apiActor)

        val (state, _) = RequestGetState() :~> <~:[ResponseSeq]
        state.state must not equalTo None
      }
    }

    "send updates in new connection" in new sqlDb {
      val (scope1, scope2) = TestScope.pair(rand.nextInt(), rand.nextInt())
      catchNewSession(scope1)
      catchNewSession(scope2)

      { // subscribe
        implicit val scope = scope1

        val (state, _) = RequestGetState() :~> <~:[ResponseSeq]
        state.state must equalTo(None)
        RequestGetState() :~> <~:[ResponseSeq]
      }

      val scope3 = scope1.reconnect()

      {
        implicit val scope = scope3

        // just send any package to auth new connection
        RequestSetOnline(false, 100) :~> <~:[ResponseVoid]
      }

      {
        implicit val scope = scope2
        val (state, _) = RequestGetState() :~> <~:[ResponseSeq]
        state.seq must equalTo(0)

        val rq = RequestSendEncryptedMessage(
          struct.OutPeer.privat(scope1.user.uid, ACL.userAccessHash(scope.user.authId, scope1.user)),
          randomId = 555L,
          encryptedMessage = BitVector(1, 2, 3),
          keys = immutable.Seq(
            EncryptedAESKey(
              scope1.user.publicKeyHash, BitVector(1, 0, 1, 0)
            )
          ),
          ownKeys = immutable.Seq(
            EncryptedAESKey(
              scope2.user.publicKeyHash, BitVector(1, 0, 1, 0)
            )
          )
        )

        rq :~> <~:[ResponseSeqDate]
      }

      {
        implicit val scope = scope1

        // Update
        protoReceiveN(1)(scope.probe, scope.apiActor)

        val (state, _) = RequestGetState() :~> <~:[ResponseSeq]
        state.state must not equalTo None
      }

      {
        implicit val scope = scope3

        // Update
        val p = protoReceiveN(1)(scope.probe, scope.apiActor)
        MessageBoxCodec.decodeValidValue(p.head.messageBoxBytes).body.assertInstanceOf[UpdateBox]
      }
    }

    "get difference" in new sqlDb {
      val (scope1, scope2) = TestScope.pair(rand.nextInt(), rand.nextInt())
      catchNewSession(scope1)
      catchNewSession(scope2)

      {
        implicit val scope = scope1

        RequestGetState() :~> <~:[ResponseSeq]

        val (state, _) = RequestGetState() :~> <~:[ResponseSeq]
        state.state must equalTo(None)
      }


      {
        implicit val scope = scope2
        val (state, _) = RequestGetState() :~> <~:[ResponseSeq]

        for (i <- (1 to 330)) {
          val rq = RequestSendEncryptedMessage(
            outPeer = struct.OutPeer.privat(scope1.user.uid, ACL.userAccessHash(scope.user.authId, scope1.user)),
            randomId = i,
            encryptedMessage = BitVector(i),
            keys = immutable.Seq(
              EncryptedAESKey(
                scope1.user.publicKeyHash, BitVector(1, 0, 1, 0)
              )
            ),
            ownKeys = immutable.Seq.empty
          )

          rq :~>!
        }
      }

      Thread.sleep(10000)

      {
        implicit val scope = scope1

        // Update
        protoReceiveN(330, DurationInt(180).seconds)(scope.probe, scope.apiActor)

        val (state, _) = RequestGetState() :~> <~:[ResponseSeq]
        state.state must not equalTo None

        val (diff1, _) = RequestGetDifference(0, None) :~> <~:[ResponseGetDifference]
        diff1.updates.length must equalTo(300)

        val (diff2, _) = RequestGetDifference(diff1.seq, diff1.state) :~> <~:[ResponseGetDifference]
        diff2.updates.length must equalTo(30)

        val updates = diff1.updates ++ diff2.updates

        val expectedMessages = (1 to 330) map { i =>
          BitVector(i)
        } toSet

        updates.map(_.body.asInstanceOf[update.EncryptedMessage].message).toSet must equalTo(expectedMessages)

        val (diff3, _) = RequestGetDifference(diff2.seq, diff2.state) :~> <~:[ResponseGetDifference]
        diff3.updates.length must equalTo(0)
        diff3.state must not equalTo(None)
      }
    }
  }
}
