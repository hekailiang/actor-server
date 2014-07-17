package com.secretapp.backend.api.auth

import akka.actor._
import scala.collection.immutable.Seq
import scala.util.{ Random, Try, Success, Failure }
import com.typesafe.config.ConfigFactory
import com.datastax.driver.core.{ Session => CSession }
import com.secretapp.backend.api.PackageCommon
import com.secretapp.backend.data.message.struct.{ User => StructUser }
import com.secretapp.backend.data.message.{TransportMessage, RpcResponseBox}
import com.secretapp.backend.data.message.rpc._
import com.secretapp.backend.data.message.rpc.auth._
import com.secretapp.backend.data.models._
import com.secretapp.backend.persist._
import com.secretapp.backend.sms.ClickatellSMSEngine
import com.secretapp.backend.data.transport._
import com.secretapp.backend.util.HandleFutureOpt._
import scodec.bits.BitVector
import scalaz._
import Scalaz._

trait SignService extends PackageCommon { self: Actor =>
  implicit val session: CSession

  import context._

  def handleRpcAuth(p: Package, messageId: Long): PartialFunction[RpcRequestMessage, Any] = {
    case r: RequestAuthCode => (handleRequestAuthCode(p, messageId) _).tupled(RequestAuthCode.unapply(r).get)
    case r: RequestSignIn => (handleRequestSignIn(p, messageId) _).tupled(RequestSignIn.unapply(r).get)
    case r: RequestSignUp => (handleRequestSignUp(p, messageId) _).tupled(RequestSignUp.unapply(r).get)
  }

  def handleRequestAuthCode(p: Package, messageId: Long)(phoneNumber: Long, appId: Int, apiKey: String): Unit = {
    val smsCode = rand.nextLong().toString.drop(1).take(6)
    val smsHash = rand.nextLong().toString

    val serverConfig = ConfigFactory.load()
    val clickatell = new ClickatellSMSEngine(serverConfig) // TODO: use singleton for share config env
    // TODO: validate number

    // #400: PHONE_NUMBER_INVALID - неверный номер телефона. Отображается пользователю.
    // TODO: Service unreachable?

    val f = for {
      _ <- clickatell.send(phoneNumber.toString, s"Your secret app activation code: $smsCode") // TODO: singleton for model
      phoneR <- PhoneRecord.getEntity(phoneNumber)
      _ <- AuthSmsCodeRecord.insertEntity(AuthSmsCode(phoneNumber, smsHash, smsCode))
    } yield ResponseAuthCode(smsHash, phoneR.isDefined)
    // TODO: DRY
    f.onComplete  {
      case Success(res) =>
        val reply = p.replyWith(messageId, RpcResponseBox(messageId, Ok(res))).right
        handleActor ! PackageToSend(reply)
      case Failure(e) =>
        val error = Error(400, "PHONE_NUMBER_INVALID", e.getMessage, true) // TODO
        val reply = p.replyWith(messageId, RpcResponseBox(messageId, error)).right
        handleActor ! PackageToSend(reply)
    }
  }

  private def validateSignParams(phoneNumber: Long,
                                 smsHash: String,
                                 smsCode: String)(smsCodeR: AuthSmsCode): Error \/ Unit = {
    smsCodeR match {
      case s if s.smsHash != smsHash =>
        Error(400, "PHONE_CODE_EXPIRED", "", true).left
      case s if s.smsCode.isEmpty =>
        Error(400, "PHONE_CODE_EMPTY", "", true).left
      case s if s.smsCode != smsCode =>
        Error(400, "PHONE_CODE_INVALID", "", true).left
      case _ => ().right
    }
  }

  def handleRequestSignIn(p: Package, messageId: Long)(phoneNumber: Long,
                                                       smsHash: String,
                                                       smsCode: String,
                                                       publicKey: BitVector): Unit =
  {
    PhoneRecord.getEntity(phoneNumber).onComplete {
      case Success(Some(phoneR)) => // TODO: remove Success(Some( for pattern matching
        AuthSmsCodeRecord.getEntity(phoneNumber).onComplete {
          case Success(Some(smsCodeR)) =>
            validateSignParams(phoneNumber, smsHash, smsCode)(smsCodeR).rightMap { _ =>
              phoneR.user.onComplete {
                case Success(Some(user)) =>
                  val sUser = StructUser(phoneR.userId, user.accessHash, user.firstName, user.lastName, user.sex, Seq(1L))
                  Ok(ResponseAuth(123L, sUser))
                case _ =>
//                  Internal error
              }
            }
          case _ =>
            val error = Error(400, "PHONE_CODE_EMPTY", "", true) // TODO
            sendPackage(p, messageId)(RpcResponseBox(messageId, error))
        }
      case _ =>
        val error = Error(400, "PHONE_NUMBER_UNOCCUPIED", "", true) // TODO
        sendPackage(p, messageId)(RpcResponseBox(messageId, error))
    }
  }

  def handleRequestSignUp(p: Package, messageId: Long)(phoneNumber: Long,
                                                       smsHash: String,
                                                       smsCode: String,
                                                       firstName: String,
                                                       lastName: Option[String],
                                                       publicKey: BitVector): Unit = {
    ???
  }

  private def sendPackage(p: Package, messageId: Long)(tm: TransportMessage): Unit = {
    val reply = p.replyWith(messageId, tm).right
    handleActor ! PackageToSend(reply)
  }
}
