package com.secretapp.backend.api

import akka.actor._
import akka.io.Tcp._
import akka.testkit._
import com.newzly.util.testing.AsyncAssertionsHelper._
import com.secretapp.backend.crypto.ec
import com.secretapp.backend.data._
import com.secretapp.backend.data.message._
import com.secretapp.backend.data.message.rpc._
import com.secretapp.backend.data.message.rpc.messaging._
import com.secretapp.backend.data.message.rpc.{ update => updateProto }
import com.secretapp.backend.data.message.update.{ SeqUpdate, GroupInvite }
import com.secretapp.backend.data.models._
import com.secretapp.backend.data.transport._
import com.secretapp.backend.data.types._
import com.secretapp.backend.persist._
import com.secretapp.backend.protocol.codecs.message.MessageBoxCodec
import com.secretapp.backend.services.common.RandomService
import com.secretapp.backend.services.GeneratorService
import com.secretapp.backend.services.rpc.RpcSpec
import java.util.UUID
import org.scalamock.specs2.MockFactory
import org.specs2.mutable.{ ActorLikeSpecification, ActorServiceHelpers }
import scala.collection.immutable
import scala.language.higherKinds
import scala.util.Random
import scalaz.Scalaz._
import scodec.bits._
import scodec.codecs.uuid

class GroupMessagingSpec extends RpcSpec {
  import system.dispatcher

  "GroupMessaging" should {
    "send invites on creation" in {
      val (scope1, scope2) = TestScope.pair()

      {
        implicit val scope = scope1

        val rq = RequestCreateChat(
          randomId = 1L,
          title = "Groupchat 3000",
          keyHash = BitVector(1, 1, 1),
          publicKey = BitVector(1, 0, 1, 0),
          invites = immutable.Seq(
            InviteUser(
              uid = scope2.user.uid,
              accessHash = User.getAccessHash(scope1.user.authId, scope2.user.uid, scope2.user.accessSalt),
              keys = immutable.Seq(
                EncryptedMessage(
                  message = BitVector(1, 2, 3),
                  keys = immutable.Seq(
                    EncryptedKey(
                      keyHash = scope2.user.publicKeyHash,
                      aesEncryptedKey = BitVector(2, 0, 2, 0)
                    )
                  )
                )
              )
            )
          )
        )
        rq :~> <~:[ResponseCreateChat]
      }

      {
        implicit val scope = scope2

        val (diff, _) = updateProto.RequestGetDifference(0, None) :~> <~:[updateProto.Difference]
        diff.updates.head.body.assertInstanceOf[GroupInvite]
      }
    }
  }
}
