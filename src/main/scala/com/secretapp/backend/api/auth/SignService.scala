package com.secretapp.backend.api.auth

import akka.actor._
import scala.collection.immutable.Seq
import scala.util.{ Random, Try, Success, Failure }
import scala.concurrent.Future
import com.typesafe.config.ConfigFactory
import com.datastax.driver.core.{ Session => CSession }
import com.secretapp.backend.api.PackageCommon
import com.secretapp.backend.data.message.struct.{ User => StructUser }
import com.secretapp.backend.data.message.{TransportMessage, RpcResponseBox}
import com.secretapp.backend.data.message.rpc._
import com.secretapp.backend.data.message.rpc.auth._
import com.secretapp.backend.data.models._
import com.secretapp.backend.data.types._
import com.secretapp.backend.persist._
import com.secretapp.backend.sms.ClickatellSMSEngine
import com.secretapp.backend.data.transport._
import com.secretapp.backend.util.HandleFutureOpt._
import PackageCommon._
import scodec.bits.BitVector
import scalaz._
import Scalaz._

trait SignService extends PackageCommon { self: Actor =>
  implicit val session: CSession

  import context._

  type RpcResult = HandleResult[Error, RpcResponseMessage]

  val internalError = Error(400, "INTERNAL_ERROR", "")

  def handleRpcAuth(p: Package, messageId: Long): PartialFunction[RpcRequestMessage, Any] = {
    case r: RequestAuthCode => sendResult(p, messageId)((handleRequestAuthCode _).tupled(RequestAuthCode.unapply(r).get))
    case r: RequestSignIn => sendResult(p, messageId)((handleRequestSignIn _).tupled(RequestSignIn.unapply(r).get))
    case r: RequestSignUp => sendResult(p, messageId)((handleRequestSignUp _).tupled(RequestSignUp.unapply(r).get))
  }

  def handleRequestAuthCode(phoneNumber: Long, appId: Int, apiKey: String): RpcResult = {
    val smsCode = rand.nextLong().toString.drop(1).take(6)
    val smsHash = rand.nextLong().toString

    val serverConfig = ConfigFactory.load()
    val clickatell = new ClickatellSMSEngine(serverConfig) // TODO: use singleton for share config env

    clickatell.send(phoneNumber.toString, s"Your secret app activation code: $smsCode") // TODO: singleton for model

    val f = for {
      phoneR <- PhoneRecord.getEntity(phoneNumber)
      _ <- AuthSmsCodeRecord.insertEntity(AuthSmsCode(phoneNumber, smsHash, smsCode))
    } yield ResponseAuthCode(smsHash, phoneR.isDefined)
    f.flatMap(r => Future.successful(r.right)).recover {
      case e: Throwable =>
        Error(400, "PHONE_NUMBER_INVALID", e.getMessage).left
    }
  }

  private def validateSignParams(smsHash: String, smsCode: String)(smsCodeR: AuthSmsCode): Error \/ Unit = {
    smsCodeR match {
      case s if s.smsHash != smsHash =>
        Error(400, "PHONE_CODE_EXPIRED", "").left
      case s if s.smsCode != smsCode =>
        Error(400, "PHONE_CODE_INVALID", "").left
      case _ => ().right
    }
  }

  def handleRequestSignIn(phoneNumber: Long, smsHash: String, smsCode: String, publicKey: BitVector): RpcResult = {
    if (smsCode.isEmpty) {
      Future.successful(Error(400, "PHONE_CODE_EMPTY", "").left)
    } else {
      for {
        phoneR <- futOptHandle(PhoneRecord.getEntity(phoneNumber), Error(400, "PHONE_NUMBER_UNOCCUPIED", ""))
        user <- futOptHandle(phoneR.user, internalError)
        smsCodeR <- futOptHandle(AuthSmsCodeRecord.getEntity(phoneNumber), Error(400, "PHONE_CODE_EXPIRED", ""))
      } yield {
        validateSignParams(smsHash, smsCode)(smsCodeR).rightMap { _ =>
          // TODO: do we need to insert new key if keyHashes not contain it?
          val sUser = StructUser(phoneR.userId, user.accessHash, user.firstName, user.lastName, user.sex.toOption, user.keyHashes)
          handleActor ! Authenticate(user)
          ResponseAuth(123L, sUser)
        }
      }
    }
  }

  def handleRequestSignUp(phoneNumber: Long, smsHash: String, smsCode: String, firstName: String, lastName: Option[String],
                          publicKey: BitVector): RpcResult = {
    if (smsCode.isEmpty) {
      Future.successful(Error(400, "PHONE_CODE_EMPTY", "").left)
    } else {
      PhoneRecord.getEntity(phoneNumber) flatMap {
        case Some(phone) => handleRequestSignIn(phoneNumber, smsHash, smsCode, publicKey)
        case None =>
          val f = for { smsCodeR <- futOptHandle(AuthSmsCodeRecord.getEntity(phoneNumber), Error(400, "PHONE_CODE_EXPIRED", "")) }
          yield validateSignParams(smsHash, smsCode)(smsCodeR)
          f flatMap {
            case \/-(res) =>
              val user = User.build(publicKey = publicKey, firstName = firstName, lastName = lastName, sex = NoSex)
              // TODO: akka service for ID's
              val userId = 1090901
              for { _ <- UserRecord.insertEntity(Entity(userId, user)) } yield {
                handleActor ! Authenticate(user)
                val sUser = StructUser(userId, user.accessHash, user.firstName, user.lastName,
                  user.sex.toOption, Seq(user.publicKeyHash))
                ResponseAuth(123L, sUser).right
              }
            case l@(-\/(_)) => Future.successful(l)
          }
      }
    }
  }

  private def sendResult(p: Package, messageId: Long)(res: RpcResult): Unit = {
    res onComplete {
      case Success(res) =>
        val message: RpcResponse = res match {
          case \/-(okRes) => Ok(okRes)
          case -\/(errorRes) => errorRes
        }
        sendReply(p.replyWith(messageId, RpcResponseBox(messageId, message)).right)
      case Failure(e) =>
        sendReply(p.replyWith(messageId, RpcResponseBox(messageId, Error(400, "INTERNAL_ERROR", ""))).right)
    }
  }
}
