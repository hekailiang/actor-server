package com.secretapp.backend.services

import akka.actor._
import akka.util._
import akka.pattern.ask
import com.secretapp.backend.data.models._
import com.secretapp.backend.persist._
import scala.concurrent._
import scala.concurrent.duration._
import scala.collection.mutable
import com.datastax.driver.core.{ Session => CSession }

trait SessionManager extends ActorLogging {
  self: Actor  =>

  import context._

  implicit val session: CSession

  private case class GetOrCreate(authId: Long, sessionId: Long)

  private val sessionManager = context.actorOf(Props(new Actor {
    var sessionFutures = new mutable.HashMap[Long, Future[Either[Long, Long]]]()

    def receive = {
      // TODO: case for forgetting sessions?
      case GetOrCreate(authId, sessionId) =>
        log.info(s"GetOrCreate $authId, $sessionId")
        val f = sessionFutures.get(sessionId) match {
          case None =>
            log.info(s"GetOrCreate Creating Future")
            val f = SessionIdRecord.getEntity(authId, sessionId).flatMap {
              case s@Some(sessionIdRecord) => Future { Left(sessionId) }
              case None =>
                SessionIdRecord.insertEntity(SessionId(authId, sessionId)).map(_ => Right(sessionId))
            }
            sessionFutures.put(sessionId, f)
            f
          case Some(f) =>
            log.info(s"GetOrCreate Returning existing Future")
            f map {
              // We already sent Right to first future processor
              case Right(sessionId) => Left(sessionId)
              case left => left
            }
        }

        val replyTo = sender()
        f map (sessionId => replyTo ! sessionId)
    }
  }))

  implicit val timeout = Timeout(5.seconds)

  /**
   * Gets existing session from database or creates new
   *
   * @param authId auth id
   * @param sessionId session id
   *
   * @return Left[Long] if existing session got or Right[Long] if new session created
   *         Perhaps we need something more convenient that Either here
   */
  protected def getOrCreateSession(authId: Long, sessionId: Long): Future[Either[Long, Long]] = {
    ask(sessionManager, GetOrCreate(authId, sessionId)).mapTo[Either[Long, Long]]
  }
}
