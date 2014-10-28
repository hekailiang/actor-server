package com.secretapp.backend.services.rpc.user

import com.secretapp.backend.data.message.rpc.ResponseVoid
import com.secretapp.backend.data.message.rpc.messaging._
import com.secretapp.backend.data.message.rpc.update.{Difference, RequestGetDifference, ResponseSeq}
import com.secretapp.backend.data.message.rpc.user.RequestEditName
import com.secretapp.backend.data.message.update._
import com.secretapp.backend.models.User
import com.secretapp.backend.persist.UserRecord
import com.secretapp.backend.services.rpc.RpcSpec
import com.websudos.util.testing.AsyncAssertionsHelper._
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
      val (scope1, scope2) = TestScope.pair(3, 4)
      catchNewSession(scope1)
      catchNewSession(scope2)

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

      val (diff2, updates2) = {
        implicit val scope = scope1

        RequestGetDifference(diff1.seq, diff1.state) :~> <~:[Difference]
      }

      updates2.length should beEqualTo(2)
      updates2.last.body.asInstanceOf[SeqUpdate].body should beAnInstanceOf[NameChanged]

      val n = diff2.users.filter(_.uid == scope2.user.uid)(0).name

      n should_== newName
    }
  }

  import system.dispatcher

  implicit val timeout = 5.seconds

  private implicit var scope: TestScope = _

  override def before = {
    scope = TestScope()
    catchNewSession(scope)
  }

  private val newName = "John The New"

  private def editNameShouldBeOk(implicit scope: TestScope) =
    RequestEditName(newName) :~> <~:[ResponseVoid]

  private def connectWithUser(u: User)(implicit scope: TestScope) = {
    val rq = RequestSendMessage(
      u.uid,
      u.accessHash(scope.user.authId),
      555L,
      message = EncryptedRSAMessage(
        encryptedMessage = BitVector(1, 2, 3),
        keys = immutable.Seq(
          EncryptedAESKey(
            u.publicKeyHash, BitVector(1, 0, 1, 0)
          )
        ),
        ownKeys = immutable.Seq.empty
      )
    )

    rq :~> <~:[ResponseSeq]

    Thread.sleep(1000)
  }
}
