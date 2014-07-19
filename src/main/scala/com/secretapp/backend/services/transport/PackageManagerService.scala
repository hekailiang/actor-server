package com.secretapp.backend.services.transport

import java.util.concurrent.ConcurrentLinkedQueue
import akka.actor.Actor
import com.secretapp.backend.data.message.{Drop, NewSession, TransportMessage}
import com.secretapp.backend.data.models.{AuthId, User}
import com.secretapp.backend.data.transport.Package
import com.secretapp.backend.persist.AuthIdRecord
import com.secretapp.backend.services.{UserManagerService, SessionManager}
import com.secretapp.backend.services.common.PackageCommon
import com.secretapp.backend.services.common.PackageCommon.{PackageToSend, Authenticate, ServiceMessage}
import scala.util.{Failure, Success}
import scalaz._
import Scalaz._
import com.datastax.driver.core.{ Session => CSession }

trait PackageManagerService extends PackageCommon with SessionManager with UserManagerService { self: Actor =>
  import context._

  implicit val session: CSession

  private var currentAuthId: Long = _
  private var currentSessionId: Long = _
  private val currentSessions = new ConcurrentLinkedQueue[Long]()

  def serviceMessagesPF: PartialFunction[ServiceMessage, Any] = {
    // TODO: become
    case Authenticate(userId: Long, u: User) =>
      log.info(s"Authenticate: $u")
      currentUser = Some(userId, u)
  }

  private def checkPackageAuth(p: Package)(f: (Package, Option[TransportMessage]) => Unit): Unit = {
    if (p.authId == 0L) { // check for auth request - simple key registration
      if (p.sessionId == 0L) {
        val newAuthId = rand.nextLong
        AuthIdRecord.insertEntity(AuthId(newAuthId, None)).onComplete {
          case Success(_) =>
            currentAuthId = newAuthId
            f(p, None)
          case Failure(e) => sendDrop(p, e)
        }
      } else {
        sendDrop(p, s"unknown session id(${p.sessionId}) within auth id(${p.authId}})")
      }
    } else {
      AuthIdRecord.getEntity(p.authId).onComplete {
        case Success(res) => res match {
          case Some(authIdRecord) =>
            currentAuthId = authIdRecord.authId
            currentUser = authIdRecord.user.map(u => (authIdRecord.userId.get, u)) // TODO: remove Entity class
            handlePackageAuthentication(p)(f)
          case None => sendDrop(p, s"unknown auth id(${p.authId}) or session id(${p.sessionId})")
        }
        case Failure(e) => sendDrop(p, e)
      }
    }
  }

  private def checkPackageSession(p: Package)(f: (Package, Option[TransportMessage]) => Unit): Unit = {
    @inline
    def updateCurrentSession(sessionId: Long): Unit = {
      if (currentSessionId == 0L) {
        currentSessionId = sessionId
      }
      currentSessions.add(sessionId)
    }

    if (p.authId == currentAuthId) {
      if (p.sessionId == 0L) {
        sendDrop(p, "sessionId can't be zero")
      } else {
        if (p.sessionId == currentSessionId || currentSessions.contains(p.sessionId)) {
          f(p, None)
        } else {
          getOrCreateSession(p.authId, p.sessionId).andThen {
            case Success(ms) => ms match {
              case Left(sessionId) =>
                updateCurrentSession(sessionId)
                f(p, None)
              case Right(sessionId) =>
                updateCurrentSession(sessionId)
                f(p, Some(NewSession(sessionId, p.messageBox.messageId)))
            }
            case Failure(e) => sendDrop(p, e)
          }
        }
      }
    } else {
      sendDrop(p, "you can't use two different auth id at the same connection")
    }
  }

  final def handlePackageAuthentication(p: Package)(f: (Package, Option[TransportMessage]) => Unit): Unit = {
    if (currentAuthId == 0L) { // check for empty auth id - it mean a new connection
      checkPackageAuth(p)(f)
    } else {
      checkPackageSession(p)(f)
    }
  }

  def getAuthId = currentAuthId

  def getSessionId = currentSessionId

  private def sendDrop(p: Package, msg: String): Unit = {
    val reply = p.replyWith(p.messageBox.messageId, Drop(p.messageBox.messageId, msg)).left
    handleActor ! PackageToSend(reply)
  }

  private def sendDrop(p: Package, e: Throwable): Unit = sendDrop(p, e.getMessage)
}