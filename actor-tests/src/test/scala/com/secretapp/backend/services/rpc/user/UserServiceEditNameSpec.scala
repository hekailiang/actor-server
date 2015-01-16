package com.secretapp.backend.services.rpc.user

import com.secretapp.backend.data.message.rpc.ResponseVoid
import com.secretapp.backend.data.message.rpc.messaging._
import com.secretapp.backend.data.message.rpc.update.{ ResponseGetDifference, RequestGetDifference, ResponseSeq }
import com.secretapp.backend.data.message.rpc.user.RequestEditName
import com.secretapp.backend.data.message.struct
import com.secretapp.backend.data.message.update._
import com.secretapp.backend.models
import com.secretapp.backend.persist
import com.secretapp.backend.services.rpc.RpcSpec
import com.secretapp.backend.util.ACL
import com.websudos.util.testing._
import org.specs2.specification.BeforeExample
import scala.collection.immutable
import scodec.bits._

class UserServiceEditNameSpec extends RpcSpec {

  "user service on receiving `RequestEditName`" should {
    "respond with `ResponseVoid`" in new sqlDb {
      scope = TestScope()
      catchNewSession(scope)

      editNameShouldBeOk
    }

    "update user name" in new sqlDb {
      scope = TestScope()
      catchNewSession(scope)

      editNameShouldBeOk

      persist.User.find(scope.user.uid)(Some(scope.user.authId)).sync.get.name should_== newName
    }

    "append update to chain" in new sqlDb {
      scope = TestScope()
      catchNewSession(scope)

      val (scope1, scope2) = TestScope.pair(3, 4)
      catchNewSession(scope1)
      catchNewSession(scope2)

      val (diff1, _) = {
        implicit val scope = scope1
        RequestGetDifference(0, None) :~> <~:[ResponseGetDifference]
      }

      {
        implicit val scope = scope2
        connectWithUser(scope1.user)
        editNameShouldBeOk
      }

      Thread.sleep(1000)

      val (diff2, _) = {
        implicit val scope = scope1

        RequestGetDifference(diff1.seq, diff1.state) :~> <~:[ResponseGetDifference]
      }

      diff2.updates.length should beEqualTo(2)
      diff2.updates.last.body.asInstanceOf[SeqUpdateMessage] should beAnInstanceOf[NameChanged]

      val n = diff2.users.filter(_.uid == scope2.user.uid)(0).name

      n should_== newName
    }
  }

  import system.dispatcher

  implicit val timeout = 5.seconds

  private implicit var scope: TestScope = _

  private val newName = "John The New"

  private def editNameShouldBeOk(implicit scope: TestScope) =
    RequestEditName(newName) :~> <~:[ResponseSeq]

  private def connectWithUser(u: models.User)(implicit scope: TestScope) = {
    val rq = RequestSendEncryptedMessage(
      struct.OutPeer.privat(u.uid, ACL.userAccessHash(scope.user.authId, u)),
      555L,
      encryptedMessage = BitVector(1, 2, 3),
      keys = immutable.Seq(
        EncryptedAESKey(
          u.publicKeyHash, BitVector(1, 0, 1, 0)
        )
      ),
      ownKeys = immutable.Seq.empty
    )

    rq :~> <~:[ResponseSeqDate]

    Thread.sleep(1000)
  }
}
