package im.actor.server.rest.controllers

import com.secretapp.backend.persist.events.LogEvent
import spray.json._
import im.actor.server.rest.models._
import im.actor.server.rest.utils.JsonImplicits._
import akka.http.marshallers.sprayjson.SprayJsonSupport._
import akka.http.server.Directives._
import akka.http.server.RequestContext
import scala.concurrent.ExecutionContext

object LogsController {
  def authLogs(ctx: RequestContext)(implicit ec: ExecutionContext) = ctx.complete {
    for { (logs, totalCount) <- AuthLog.paginate(ctx.request.uri.query.toMap) }
    yield toJson(logs, totalCount)
  }

  def stats()(implicit ec: ExecutionContext) = complete {
    for { entries <- LogEvent.stats() }
    yield JsArray(entries.map(_.toJson).toVector)
  }
}
