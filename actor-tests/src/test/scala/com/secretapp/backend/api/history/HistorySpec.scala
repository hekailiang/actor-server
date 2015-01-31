package com.secretapp.backend.api.history

import com.secretapp.backend.data.message._
import com.secretapp.backend.data.message.struct
import com.secretapp.backend.data.message.rpc._
import com.secretapp.backend.data.message.rpc.messaging._
import com.secretapp.backend.data.message.rpc.history._
import com.secretapp.backend.data.message.rpc.update._
import com.secretapp.backend.models
import com.secretapp.backend.persist
import com.secretapp.backend.services.rpc.RpcSpec
import com.secretapp.backend.util.ACL
import im.actor.testkit.ActorSpecification
import org.specs2.specification.Step

class HistorySpec extends RpcSpec {
  object sqlDb extends sqlDb

  override def is = sequential ^ s2"""
    RequestDeleteMessage handler should
      respond with ResponseVoid         ${deleteMessageCases.e1}
      delete message from history       ${deleteMessageCases.e2}
  """

  object deleteMessageCases extends sqlDb {
    val (scope1, scope2) = TestScope.pair(rand.nextInt, rand.nextInt)
    catchNewSession(scope1)

    val outPeer = struct.OutPeer.privat(scope2.user.uid, ACL.userAccessHash(scope1.user.authId, scope2.user))

    def e1 = {
      using(scope1) { implicit scope =>
        RequestSendMessage(
          outPeer = outPeer,
          randomId = 1L,
          message = TextMessage("Yolo1")
        ) :~> <~:[ResponseSeqDate]

        RequestSendMessage(
          outPeer = outPeer,
          randomId = 2L,
          message = TextMessage("Yolo2")
        ) :~> <~:[ResponseSeqDate]

        RequestSendMessage(
          outPeer = outPeer,
          randomId = 3L,
          message = TextMessage("Yolo3")
        ) :~> <~:[ResponseSeqDate]

        RequestDeleteMessage(outPeer, Vector(1L, 3L)) :~> <~:[ResponseVoid]
      }
    }

    def e2 = {
      Thread.sleep(1000)

      using(scope1) { implicit scope =>
        val (history, _) = RequestLoadHistory(outPeer, 0L, 3) :~> <~:[ResponseLoadHistory]
        history.history.length should_==(1)
        history.history.head.randomId should_==(2L)
      }
    }
  }
}
