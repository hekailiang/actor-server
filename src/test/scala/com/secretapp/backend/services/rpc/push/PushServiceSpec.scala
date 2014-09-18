package com.secretapp.backend.services.rpc.push

import com.secretapp.backend.data.models.GooglePushCredentials
import com.secretapp.backend.persist.GooglePushCredentialsRecord
import com.secretapp.backend.services.rpc.RpcSpec
import com.secretapp.backend.data.message.rpc.push._
import com.secretapp.backend.data.message.rpc.ResponseVoid
import com.newzly.util.testing.AsyncAssertionsHelper._
import scalaz._
import Scalaz._

class PushServiceSpec extends RpcSpec {
  import system.dispatcher

  "push service" should {

    "response to `RequestRegisterGooglePush` with `ResponseVoid` and store credentials passed" in {
      implicit val scope = TestScope()

      val creds = GooglePushCredentials(scope.user.uid, scope.user.authId, 42, "registration id")

      RequestRegisterGooglePush(creds.projectId, creds.regId) :~> <~:[ResponseVoid]

      GooglePushCredentialsRecord.get(creds.uid, creds.authId).sync should_== creds.some
    }

    "return ResponseVoid for RequestUnregisterPush request" in {
      implicit val scope = TestScope()

      RequestUnregisterPush() :~> <~:[ResponseVoid]
    }
  }
}
