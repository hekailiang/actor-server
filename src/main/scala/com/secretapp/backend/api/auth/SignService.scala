package com.secretapp.backend.api.auth

import akka.actor._
import com.secretapp.backend.data.message.RpcResponseBox
import scala.util.{ Random, Try, Success, Failure }
import com.typesafe.config.ConfigFactory
import com.datastax.driver.core.{ Session => CSession }
import com.secretapp.backend.api.PackageCommon
import com.secretapp.backend.data.message.rpc._
import com.secretapp.backend.data.message.rpc.auth._
import com.secretapp.backend.data.models._
import com.secretapp.backend.persist._
import com.secretapp.backend.sms.ClickatellSMSEngine
import com.secretapp.backend.data.transport._
import scalaz._
import Scalaz._

//TODO: move to single file
//trait SessionManager extends ActorLogging {
//  self: Actor  =>
//
//  import context._
//
//  implicit val session: CSession
//
//  private case class GetOrCreate(authId: Long, sessionId: Long)
//
//  private val sessionManager = context.actorOf(Props(new Actor {
//    var sessionFutures = new mutable.HashMap[Long, Future[Either[Long, Long]]]()
//
//    def receive = {
//      // TODO: case for forgetting sessions?
//      case GetOrCreate(authId, sessionId) =>
//        log.info(s"GetOrCreate $authId, $sessionId")
//        val f = sessionFutures.get(sessionId) match {
//          case None =>
//            log.info(s"GetOrCreate Creating Future")
//            val f = SessionIdRecord.getEntity(authId, sessionId).flatMap {
//              case s@Some(sessionIdRecord) => Future { Left(sessionId) }
//              case None =>
//                SessionIdRecord.insertEntity(SessionId(authId, sessionId)).map(_ => Right(sessionId))
//            }
//            sessionFutures.put(sessionId, f)
//            f
//          case Some(f) =>
//            log.info(s"GetOrCreate Returning existing Future")
//            f map {
//              // We already sent Right to first future processor
//              case Right(sessionId) => Left(sessionId)
//              case left => left
//            }
//        }
//
//        val replyTo = sender()
//        f map (sessionId => replyTo ! sessionId)
//    }
//  }))
//
//  implicit val timeout = Timeout(5 seconds)
//
//  /**
//   * Gets existing session from database or creates new
//   *
//   * @param authId auth id
//   * @param sessionId session id
//   *
//   * @return Left[Long] if existing session got or Right[Long] if new session created
//   *         Perhaps we need something more convenient that Either here
//   */
//  protected def getOrCreateSession(authId: Long, sessionId: Long): Future[Either[Long, Long]] = {
//    ask(sessionManager, GetOrCreate(authId, sessionId)).mapTo[Either[Long, Long]]
//  }
//}

trait SignService extends PackageCommon { self: Actor =>
  implicit val session: CSession

  import context._

  def handleRpcAuth(p: Package, messageId: Long): PartialFunction[RpcRequestMessage, Any] = {
    case r: RequestAuthCode => (handleRequestAuthCode(p, messageId) _).tupled(RequestAuthCode.unapply(r).get)
    case r: RequestSignIn => ???
    case r: RequestSignUp => ???
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
        val error = Error(0, e.getMessage, e.getMessage, true) // TODO
        val reply = p.replyWith(messageId, RpcResponseBox(messageId, error)).right
        handleActor ! PackageToSend(reply)
    }
  }
}
