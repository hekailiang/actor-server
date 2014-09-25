package com.secretapp.backend.services.rpc.user

import com.newzly.util.testing.AsyncAssertionsHelper._
import com.secretapp.backend.data.message.rpc.ResponseVoid
import com.secretapp.backend.data.message.rpc.user.RequestUpdateUser
import com.secretapp.backend.persist.UserRecord
import com.secretapp.backend.services.rpc.RpcSpec
import org.specs2.specification.BeforeExample

class UserServiceUpdateUserSpec extends RpcSpec with BeforeExample  {

  "user service on receiving `RequestUpdateUser`" should {

    "respond with `ResponseVoid`" in {
      updateUserShouldBeOk
    }

    "update user name" in {
      updateUserShouldBeOk

      UserRecord.getEntity(scope.user.uid, scope.user.authId).sync.get.name should_== newName
    }
  }

  import system.dispatcher

  implicit val timeout = 5.seconds

  private implicit var scope: TestScope = _

  override def before = {
    scope = TestScope()
  }

  private val newName = "John The New"

  private def updateUserShouldBeOk(implicit scope: TestScope) =
    RequestUpdateUser(newName) :~> <~:[ResponseVoid]

}
