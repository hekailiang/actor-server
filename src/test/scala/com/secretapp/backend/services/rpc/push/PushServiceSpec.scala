package com.secretapp.backend.services.rpc.push

import com.secretapp.backend.data.models.GooglePushCredentials
import com.secretapp.backend.persist.GooglePushCredentialsRecord
import com.secretapp.backend.services.rpc.RpcSpec
import com.secretapp.backend.data.message.rpc.push._
import com.secretapp.backend.data.message.rpc.ResponseVoid
import com.newzly.util.testing.AsyncAssertionsHelper._
import org.specs2.specification.BeforeExample
import scalaz._
import Scalaz._

class PushServiceSpec extends RpcSpec with BeforeExample {

  "push service" should {

    "respond to `RequestRegisterGooglePush` with `ResponseVoid`" in {
      registerShouldBeOk
    }

    "store credentials passed on `RequestRegisterGooglePush` request" in {
      registerShouldBeOk
      storedCreds should_== creds.some
    }

    "respond to `RequestUnregisterPush` with OK even if device is not registered first" in {
      unregisterShouldBeOk
    }

    "remove credentials on `RequestUnregisterPush`" in {
      registerShouldBeOk
      unregisterShouldBeOk
      storedCreds should_== None
    }
  }

  private implicit var scope: TestScope = _

  override def before = {
    scope = TestScope()
    catchNewSession(scope)
  }

  private def creds(implicit scope: TestScope) = GooglePushCredentials(scope.user.uid, scope.user.authId, 42, "reg id")

  private def storedCreds(implicit scope: TestScope) = GooglePushCredentialsRecord.get(creds.uid, creds.authId).sync()

  private def registerShouldBeOk(implicit scope: TestScope) =
    RequestRegisterGooglePush(creds.projectId, creds.regId) :~> <~:[ResponseVoid]

  private def unregisterShouldBeOk(implicit scope: TestScope) = RequestUnregisterPush() :~> <~:[ResponseVoid]
}
