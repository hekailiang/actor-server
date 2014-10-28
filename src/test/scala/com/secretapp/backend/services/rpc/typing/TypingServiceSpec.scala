package com.secretapp.backend.services.rpc.typing

import akka.actor._
import akka.testkit._
import com.secretapp.backend.data.message.{ UpdateBox, RpcResponseBox }
import com.secretapp.backend.data.message.rpc.{ Ok, ResponseVoid }
import com.secretapp.backend.data.message.rpc.messaging._
import com.secretapp.backend.data.message.rpc.update._
import com.secretapp.backend.data.message.rpc.typing._
import com.secretapp.backend.data.message.struct.UserId
import com.secretapp.backend.data.message.update
import com.secretapp.backend.data.message.update._
import com.secretapp.backend.data.message.update.WeakUpdate
import com.secretapp.backend.models.User
import com.secretapp.backend.protocol.codecs.message.MessageBoxCodec
import com.secretapp.backend.services.rpc.RpcSpec
import scala.collection.immutable
import scala.concurrent.duration._
import scodec.bits._

class TypingServiceSpec extends RpcSpec {
  import system.dispatcher

  "presence service" should {
    "send typings on subscribtion and receive typing weak updates" in {
      val (scope1, scope2) = TestScope.pair(1, 2)
      catchNewSession(scope1)
      catchNewSession(scope2)

      {
        implicit val scope = scope1

        RequestTyping(scope2.user.uid, scope2.user.accessHash(scope.user.authId), 1) :~> <~:[ResponseVoid]
      }

      {
        implicit val scope = scope2

        val (_, updates) = RequestGetState() :~> <~:[ResponseSeq]

        val update = updates match {
          case Nil => receiveNMessageBoxes(1)(scope.probe, scope.apiActor).head.body
          case xs => xs.head
        }

        update should beAnInstanceOf[UpdateBox]
        update.asInstanceOf[UpdateBox].body should beAnInstanceOf[WeakUpdate]
      }
    }

    "send typings weak updates" in {
      val (scope1, scope2) = TestScope.pair(1, 2)
      catchNewSession(scope1)
      catchNewSession(scope2)

      {
        implicit val scope = scope1

        RequestGetState() :~> <~:[ResponseSeq]

      }

      {
        implicit val scope = scope2

        RequestTyping(scope1.user.uid, scope1.user.accessHash(scope.user.authId), 1) :~> <~:[ResponseVoid]
      }

      {
        implicit val scope = scope1

        val update = receiveNMessageBoxes(1)(scope.probe, scope.apiActor).head.body
        update should beAnInstanceOf[UpdateBox]
        update.asInstanceOf[UpdateBox].body should beAnInstanceOf[WeakUpdate]
      }
    }

    "send group typings weak updates" in {
      val (scope1, scope2) = TestScope.pair(3, 4)
      catchNewSession(scope1)
      catchNewSession(scope2)

      val respGroup = {
        implicit val scope = scope1

        val rqCreateGroup = RequestCreateGroup(
          randomId = 1L,
          title = "Groupgroup 3000",
          keyHash = BitVector(1, 2, 3),
          publicKey = BitVector(1, 0, 1, 0),
          broadcast = EncryptedRSABroadcast(
            encryptedMessage = BitVector(1, 2, 3),
            keys = immutable.Seq.empty,
            ownKeys = immutable.Seq(
              EncryptedAESKey(
                keyHash = scope.user.publicKeyHash,
                aesEncryptedKey = BitVector(2, 0, 2, 0)
              )
            )
          )
        )
        val (resp, _) = rqCreateGroup :~> <~:[ResponseCreateGroup]

        RequestGetState() :~> <~:[ResponseSeq]

        resp
      }

      {
        implicit val scope = scope2

        RequestGroupTyping(respGroup.groupId, respGroup.accessHash, 1) :~> <~:[ResponseVoid]
      }

      {
        implicit val scope = scope1

        val update = receiveNMessageBoxes(1)(scope.probe, scope.apiActor).head.body
        update.assertInstanceOf[UpdateBox].body.assertInstanceOf[WeakUpdate].body.assertInstanceOf[TypingGroup]
      }
    }
  }
}
