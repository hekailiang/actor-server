package com.secretapp.backend.services.rpc.user

import com.newzly.util.testing.AsyncAssertionsHelper._
import com.secretapp.backend.data.message.rpc.ResponseVoid
import com.secretapp.backend.data.message.rpc.messaging._
import com.secretapp.backend.data.message.rpc.update.{Difference, RequestGetDifference, ResponseSeq}
import com.secretapp.backend.data.message.rpc.user.RequestEditName
import com.secretapp.backend.data.message.update._
import com.secretapp.backend.data.models.User
import com.secretapp.backend.persist.UserRecord
import com.secretapp.backend.services.rpc.RpcSpec
import org.specs2.specification.BeforeExample
import scala.collection.immutable
import scodec.bits._

class UserServiceEditNameSpec extends RpcSpec with BeforeExample  {

  "user service on receiving `RequestEditName`" should {

    "respond with `ResponseVoid`" in {
      editNameShouldBeOk
    }

    "update user name" in {
      editNameShouldBeOk

      UserRecord.getEntity(scope.user.uid, scope.user.authId).sync.get.name should_== newName
    }

    "append update to chain" in {
      val (scope1, scope2) = TestScope.pair(1, 2)

      val (diff1, _) = {
        implicit val scope = scope1
        RequestGetDifference(0, None) :~> <~:[Difference]
      }

      {
        implicit val scope = scope2
        connectWithUser(scope1.user)
        editNameShouldBeOk
      }

      Thread.sleep(1000)

      val (diff2, _) = {
        implicit val scope = scope1
        protoReceiveN(1)(scope.probe, scope.apiActor)
        RequestGetDifference(diff1.seq, diff1.state) :~> <~:[Difference]
      }

      diff2.updates.last.body should beAnInstanceOf[NameChanged]
      val n = diff2.users.filter(_.uid == scope2.user.uid)(0).name

      n should_== newName
    }
  }

  import system.dispatcher

  implicit val timeout = 5.seconds

  private implicit var scope: TestScope = _

  override def before = {
    scope = TestScope()
  }

  private val newName = "John The New"

  private def editNameShouldBeOk(implicit scope: TestScope) =
    RequestEditName(newName) :~> <~:[ResponseVoid]

  private def connectWithUser(u: User)(implicit scope: TestScope) = {
    val rq = RequestSendMessage(
      u.uid,
      u.accessHash(scope.user.authId),
      555L,
      message = EncryptedMessage(
        message = BitVector(1, 2, 3),
        keys = immutable.Seq(
          EncryptedKey(
            u.publicKeyHash, BitVector(1, 0, 1, 0)
          )
        )
      ), selfMessage = None
    )

    rq :~> <~:[ResponseSeq]
  }
}
