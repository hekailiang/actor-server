package com.secretapp.backend.services.rpc.updates

import akka.actor._
import akka.testkit._
import com.secretapp.backend.api.ApiHandlerActor
import com.secretapp.backend.data.message.rpc.update._
import com.secretapp.backend.data.models.User
import com.secretapp.backend.services.rpc.RpcSpec
import scodec.codecs.{ int32 => int32codec }
import scodec.bits._

class UpdatesServiceSpec extends RpcSpec {
  import system.dispatcher

  "updates service" should {
    "get empty state" in {
      // First user
      implicit val scope = RpcTestScope()

      val state = RequestGetState() :~> <~:[State]
      state.state must equalTo(None)
    }
  }
}
