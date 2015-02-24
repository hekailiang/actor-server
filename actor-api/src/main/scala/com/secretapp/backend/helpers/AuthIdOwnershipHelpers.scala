package com.secretapp.backend.helpers

import akka.event.LoggingAdapter
import com.secretapp.backend.models
import com.secretapp.backend.persist
import scala.concurrent._

trait AuthIdOwnershipHelpers {
  def log: LoggingAdapter
  private var userIdsMap: Map[Long, Future[Int]] = Map.empty

  protected final def getOrSetUserId(authId: Long)(implicit ec: ExecutionContext): Future[Int] = {
    userIdsMap.get(authId) getOrElse {
      log.debug("Resolving userId for authId: {}", authId)
      val f = for {
        authIdOpt <- persist.AuthId.find(authId)
      } yield {
        authIdOpt match {
          case Some(models.AuthId(_, auserIdOpt)) =>
            auserIdOpt map { id =>
              id
            } getOrElse {
              throw new Exception("AuthId's userId null while trying to set userId")
            }
          case None =>
            throw new Exception("AuthId was not found")
        }
      }

      f onFailure {
        case e: Throwable =>
          log.error(e, "Failed to resolve userId")
      }

      userIdsMap = userIdsMap + ((authId, f))
      f
    }
  }
}
