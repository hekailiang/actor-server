package com.secretapp.backend.services.rpc.auth

import akka.actor._
import scala.collection.immutable.Seq
import scala.util.{ Random, Try, Success, Failure }
import scala.concurrent.Future
import com.typesafe.config.ConfigFactory
import com.datastax.driver.core.{ Session => CSession }
import com.secretapp.backend.services.common.PackageCommon
import com.secretapp.backend.services.common.PackageCommon._
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
import com.secretapp.backend.services.GeneratorService
import com.secretapp.backend.services.rpc.RpcCommon
import scodec.bits.BitVector
import scalaz._
import Scalaz._

trait SignService extends PackageCommon with RpcCommon { self: Actor with GeneratorService =>
  implicit val session: CSession

  import context._

  def handleRpcAuth(p: Package, messageId: Long): PartialFunction[RpcRequestMessage, Any] = {
    case r: RequestAuthCode => sendRpcResult(p, messageId)((handleRequestAuthCode _).tupled(RequestAuthCode.unapply(r).get))
    case r: RequestSignIn => sendRpcResult(p, messageId)((handleRequestSignIn _).tupled(RequestSignIn.unapply(r).get))
    case r: RequestSignUp => sendRpcResult(p, messageId)((handleRequestSignUp _).tupled(RequestSignUp.unapply(r).get))
  }

  def handleRequestAuthCode(phoneNumber: Long, appId: Int, apiKey: String): RpcResult = {
    val smsCode = genSmsCode
    val smsHash = genSmsHash
    val serverConfig = ConfigFactory.load()
    val clickatell = new ClickatellSMSEngine(serverConfig) // TODO: use singleton for share config env

    clickatell.send(phoneNumber.toString, s"Your secret app activation code: $smsCode") // TODO: move it to actor with persistence

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
          val sUser = StructUser(phoneR.userId, user.selfAccessHash, user.firstName, user.lastName, user.sex.toOption,
            user.keyHashes)
          handleActor ! Authenticate(user)
          ResponseAuth(user.publicKeyHash, sUser)
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
          val f =
            for { smsCodeR <- futOptHandle(AuthSmsCodeRecord.getEntity(phoneNumber), Error(400, "PHONE_CODE_EXPIRED", "")) }
            yield validateSignParams(smsHash, smsCode)(smsCodeR)

          f flatMap {
            case \/-(_) =>
              val userId = genUserId
              val accessSalt = genUserAccessSalt
              val user = User.build(uid = userId, publicKey = publicKey, accessSalt = accessSalt, firstName = firstName,
                lastName = lastName)
              for { _ <- UserRecord.insertEntity(user) } yield {
                handleActor ! Authenticate(user)
                val sUser = StructUser(userId, user.selfAccessHash, user.firstName, user.lastName,
                  user.sex.toOption, Seq(user.publicKeyHash)) // TODO: move into User model
                ResponseAuth(user.publicKeyHash, sUser).right
              }
            case l@(-\/(_)) => Future.successful(l)
          }
      }
    }
  }
}
