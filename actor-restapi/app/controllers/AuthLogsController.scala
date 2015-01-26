package controllers

import com.secretapp.backend.persist.events.LogEvent
import utils.JsonImplicits._
import play.api.mvc._
import play.api.libs.json._
import play.api.libs.concurrent.Execution.Implicits.defaultContext

object AuthLogsController extends Controller {

  implicit val logEventWrites = new Writes[LogEvent] {
    def writes(e: LogEvent) = JsObject(Seq(
      ("id", JsNumber(e.id)),
      ("authId", JsString(e.authId.toString)),
      ("phoneNumber", JsString(e.phoneNumber.toString)),
      ("email", JsString(e.email)),
      ("klass", JsNumber(e.klass)),
      ("body", Json.parse(e.jsonBody)),
      ("createdAt", JsString(e.createdAt.toDateTimeISO.toString))
    ))
  }

  def index() = Action.async { req =>
    for { (events, totalCount) <- LogEvent.all(req.queryString) }
    yield Ok(toJson(events, totalCount))
  }

}
