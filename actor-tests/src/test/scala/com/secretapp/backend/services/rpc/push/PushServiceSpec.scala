package com.secretapp.backend.services.rpc.push

import com.secretapp.backend.models
import com.secretapp.backend.persist
import com.secretapp.backend.services.rpc.RpcSpec
import com.secretapp.backend.data.message.rpc.push._
import com.secretapp.backend.data.message.rpc.ResponseVoid
import com.websudos.util.testing._
import org.specs2.specification.BeforeExample
import scalaz._
import Scalaz._

class PushServiceSpec extends RpcSpec {

  "push service" should {
    "respond to `RequestRegisterGooglePush` with `ResponseVoid`" in new sqlDb {
      implicit val scope = TestScope()
      catchNewSession(scope)

      registerGoogleShouldBeOk
      registerAppleShouldBeOk
    }

    "store credentials passed on `RequestRegisterGooglePush` request" in new sqlDb {
      implicit val scope = TestScope()
      catchNewSession(scope)

      registerGoogleShouldBeOk
      storedGoogleCreds should_== googleCreds.some
    }

    "respond to `RequestUnregisterPush` with OK even if device is not registered first" in new sqlDb {
      implicit val scope = TestScope()
      catchNewSession(scope)

      unregisterShouldBeOk
    }

    "remove credentials on `RequestUnregisterPush`" in new sqlDb {
      implicit val scope = TestScope()
      catchNewSession(scope)

      registerGoogleShouldBeOk
      unregisterShouldBeOk
      storedGoogleCreds should_== None
    }
  }

  private def googleCreds(implicit scope: TestScope) = models.GooglePushCredentials(scope.user.authId, 42, "reg id")
  private def appleCreds(implicit scope: TestScope) = models.ApplePushCredentials(scope.user.authId, 42, "token")

  private def storedGoogleCreds(implicit scope: TestScope) = persist.GooglePushCredentials.find(googleCreds.authId).sync()

  private def registerGoogleShouldBeOk(implicit scope: TestScope) =
    RequestRegisterGooglePush(googleCreds.projectId, googleCreds.regId) :~> <~:[ResponseVoid]

  private def registerAppleShouldBeOk(implicit scope: TestScope) =
    RequestRegisterApplePush(appleCreds.apnsKey, appleCreds.token) :~> <~:[ResponseVoid]

  private def unregisterShouldBeOk(implicit scope: TestScope) = RequestUnregisterPush() :~> <~:[ResponseVoid]
}
