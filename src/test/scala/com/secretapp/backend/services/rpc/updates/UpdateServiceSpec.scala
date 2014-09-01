package com.secretapp.backend.services.rpc.updates

import akka.actor._
import akka.testkit._
import com.secretapp.backend.api.ApiHandlerActor
import com.secretapp.backend.data.message.rpc.messaging._
import com.secretapp.backend.data.message.rpc.update._
import com.secretapp.backend.data.models.User
import com.secretapp.backend.services.rpc.RpcSpec
import scodec.codecs.{ int32 => int32codec }
import scodec.bits._
import scala.collection.immutable

class UpdatesServiceSpec extends RpcSpec {
  import system.dispatcher

  "updates service" should {
    "not subscribe to updates twice" in {
      val (scope1, scope2) = RpcTestScope.pair

      {
        implicit val scope = scope1

        RequestGetState() :~> <~:[State]

        // should not cause double subscribe
        val state = RequestGetState() :~> <~:[State]
        state.state must equalTo(None)
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
        scope.probe.receiveN(1)

        val state = RequestGetState() :~> <~:[State]
        state.state must not equalTo(None)
      }
    }
  }
}
