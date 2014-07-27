package com.secretapp.backend.services.rpc.auth

import akka.actor._
import com.secretapp.backend.data.Implicits._
import scala.collection.immutable
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
import com.secretapp.backend.crypto.ec
import scodec.bits.BitVector
import scalaz._
import Scalaz._

trait SignService extends PackageCommon with RpcCommon { self: Actor with GeneratorService =>
  implicit val session: CSession

  import context._

  def handleRpcAuth(p: Package, messageId: Long): PartialFunction[RpcRequestMessage, Any] = {
    case r: RequestAuthCode => sendRpcResult(p, messageId)((handleRequestAuthCode _).tupled(RequestAuthCode.unapply(r).get))
    case r: RequestSignIn =>
      sendRpcResult(p, messageId)(handleSign(p)(r.phoneNumber, r.smsHash, r.smsCode, r.publicKey)(r.left))
    case r: RequestSignUp =>
      sendRpcResult(p, messageId)(handleSign(p)(r.phoneNumber, r.smsHash, r.smsCode, r.publicKey)(r.right))
  }

  def handleRequestAuthCode(phoneNumber: Long, appId: Int, apiKey: String): RpcResult = {
//    TODO: validate phone number

    val smsCode = genSmsCode
    val smsHash = genSmsHash
    val serverConfig = ConfigFactory.load()
    val clickatell = new ClickatellSMSEngine(serverConfig) // TODO: use singleton for share config env

    clickatell.send(phoneNumber.toString, s"Your secret app activation code: $smsCode") // TODO: move it to actor with persistence
    AuthSmsCodeRecord.insertEntity(AuthSmsCode(phoneNumber, smsHash, smsCode))

    for { phoneR <- PhoneRecord.getEntity(phoneNumber) }
    yield ResponseAuthCode(smsHash, phoneR.isDefined).right
  }

  private def handleSign(p: Package)(phoneNumber: Long, smsHash: String, smsCode: String, publicKey: BitVector)
                (m: RequestSignIn \/ RequestSignUp): RpcResult = {
    val authId = p.authId // TODO

    @inline
    def auth(u: User) = {
      handleActor ! Authenticate(u)
      ResponseAuth(u.publicKeyHash, u.toStruct(authId)).right
    }

    @inline
    def signIn(userId: Int): RpcResult = {
      @inline
      def updateUserRecord(): Unit = m match {
        case -\/(_: RequestSignIn) =>
          UserRecord.insertPartEntityWithPhoneAndPK(userId, authId, publicKey, phoneNumber)
        case \/-(req: RequestSignUp) =>
          UserRecord.insertPartEntityWithPhoneAndPK(userId, authId, publicKey, phoneNumber, req.firstName,
            req.lastName)
      }

      val publicKeyHash = ec.PublicKey.keyHash(publicKey)
      for {
        userAuthR <- UserRecord.getEntity(userId, authId)
        userR <- UserRecord.getEntity(userId) // remove it when it cause bottleneck
      } yield {
        if (userR.isEmpty) internalError.left
        else userAuthR match {
          case None =>
            updateUserRecord()
            val user = userR.get
            val keyHashes = user.keyHashes :+ publicKeyHash
            val newUser = user.copy(publicKey = publicKey, publicKeyHash = publicKeyHash, keyHashes = keyHashes)
            auth(newUser)
          case Some(userAuth) =>
            if (userAuth.publicKey != publicKey) {
              UserRecord.removeKeyHash(userId, userAuth.publicKeyHash, phoneNumber)
              updateUserRecord()
              val keyHashes = userAuth.keyHashes.filter(_ != userAuth.publicKeyHash) :+ publicKeyHash
              val newUser = userAuth.copy(publicKey = publicKey, publicKeyHash = publicKeyHash, keyHashes = keyHashes)
              auth(newUser)
            } else auth(userAuth)
        }
      }
    }

    if (smsCode.isEmpty) Future.successful(Error(400, "PHONE_CODE_EMPTY", "", false).left)
    else if (!ec.PublicKey.isPrime192v1(publicKey)) Future.successful(Error(400, "INVALID_KEY", "", false).left)
    else {
      val f = for {
        smsCodeR <- AuthSmsCodeRecord.getEntity(phoneNumber)
        phoneR <- PhoneRecord.getEntity(phoneNumber)
      } yield (smsCodeR, phoneR)
      f.flatMap { t =>
        val (smsCodeR, phoneR) = t
        if (smsCodeR.isEmpty) Future.successful(Error(400, "PHONE_CODE_EXPIRED", "", false).left)
        else smsCodeR.get match {
          case s if s.smsHash != smsHash => Future.successful(Error(400, "PHONE_CODE_EXPIRED", "", false).left)
          case s if s.smsCode != smsCode => Future.successful(Error(400, "PHONE_CODE_INVALID", "", false).left)
          case _ =>
            AuthSmsCodeRecord.dropEntity(phoneNumber)
            m match {
              case -\/(_: RequestSignIn) => phoneR match {
                case None => Future.successful(Error(400, "PHONE_NUMBER_UNOCCUPIED", "", false).left)
                case Some(rec) => signIn(rec.userId) // user must be persisted before sign in
              }
              case \/-(req: RequestSignUp) =>
                AuthSmsCodeRecord.dropEntity(phoneNumber)
                phoneR match {
                  case None =>
                    val userId = genUserId
                    val accessSalt = genUserAccessSalt
                    val user = User.build(uid = userId, authId = authId, publicKey = publicKey, accessSalt = accessSalt,
                      firstName = req.firstName, lastName = req.lastName)
                    UserRecord.insertEntityWithPhoneAndPK(user, phoneNumber)
                    Future.successful(auth(user))
                  case Some(rec) => signIn(rec.userId)
                }
            }
        }
      }
    }
  }
}
