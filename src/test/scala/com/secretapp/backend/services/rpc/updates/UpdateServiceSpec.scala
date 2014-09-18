package com.secretapp.backend.services.rpc.updates

import akka.actor._
import akka.testkit._
import com.secretapp.backend.data.message.rpc.messaging._
import com.secretapp.backend.data.message.rpc.update.RequestGetDifference
import com.secretapp.backend.data.message.update
import com.secretapp.backend.data.message.rpc.update._
import com.secretapp.backend.data.models.User
import com.secretapp.backend.services.rpc.RpcSpec
import scodec.codecs.{ int32 => int32codec }
import scodec.bits._
import scala.collection.immutable
import scala.concurrent.duration._

class UpdatesServiceSpec extends RpcSpec {
  import system.dispatcher

  "updates service" should {
    "not subscribe to updates twice" in {
      val (scope1, scope2) = TestScope.pair(1, 2)

      {
        implicit val scope = scope1

        val state = RequestGetState() :~> <~:[State]
        state.state must equalTo(None)
        RequestGetState() :~> <~:[State]
      }

      {
        implicit val scope = scope2
        val state = RequestGetState() :~> <~:[State]
        state.state must equalTo(None)

        val rq = RequestSendMessage(
          uid = scope1.user.uid, accessHash = scope1.user.accessHash(scope.user.authId),
          randomId = 555L, useAesKey = false,
          aesMessage = None,
          messages = immutable.Seq(
            EncryptedMessage(uid = scope1.user.uid, publicKeyHash = scope1.user.publicKeyHash, None, Some(BitVector(1, 2, 3)))))

        rq :~> <~:[ResponseSendMessage]
      }

      {
        implicit val scope = scope1

        // Update
        protoReceiveN(1)(scope.probe, scope.apiActor)

        val state = RequestGetState() :~> <~:[State]
        state.state must not equalTo None
      }
    }

    "get difference" in {
      val (scope1, scope2) = TestScope.pair(3, 4)

      {
        implicit val scope = scope1

        RequestGetState() :~> <~:[State]

        val state = RequestGetState() :~> <~:[State]
        state.state must equalTo(None)
      }


      {
        implicit val scope = scope2
        val state = RequestGetState() :~> <~:[State]

        for (i <- (1 to 330)) {
          val rq = RequestSendMessage(
            uid = scope1.user.uid, accessHash = scope1.user.accessHash(scope.user.authId),
            randomId = i, useAesKey = false,
            aesMessage = None,
            messages = immutable.Seq(
              EncryptedMessage(uid = scope1.user.uid, publicKeyHash = scope1.user.publicKeyHash, None, Some(BitVector(i)))))
          rq :~>!
        }
      }

      {
        implicit val scope = scope1

        // Update
        protoReceiveN(330, DurationInt(180).seconds)(scope.probe, scope.apiActor)

        val state = RequestGetState() :~> <~:[State]
        state.state must not equalTo (None)

        val diff1 = RequestGetDifference(0, None) :~> <~:[Difference]
        diff1.updates.length must equalTo(300)

        val diff2 = RequestGetDifference(diff1.seq, diff1.state) :~> <~:[Difference]
        diff2.updates.length must equalTo(30)

        val updates = diff1.updates ++ diff2.updates

        val expectedMessages = (1 to 330) map (BitVector(_)) toSet

        updates.map(_.body.asInstanceOf[update.Message].message).toSet must equalTo(expectedMessages)

        val diff3 = RequestGetDifference(diff2.seq, diff2.state) :~> <~:[Difference]
        diff3.updates.length must equalTo(0)
        diff3.state must not equalTo(None)
      }
    }
  }
}
