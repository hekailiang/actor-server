package controllers

import models._
import utils.JsonImplicits._
import play.api.mvc._
import play.api.libs.json.Json
import play.api.libs.concurrent.Execution.Implicits.defaultContext

object LogsController extends Controller {
  def authLogs() = Action.async { req =>
    for { (logs, totalCount) <- AuthLog.paginate(req.queryString) }
    yield Ok(toJson(logs, totalCount))
  }

  def stats() = Action.async { req =>
    for { stats <- StatLogs.stats() }
    yield Ok(Json.toJson(stats))
  }
}
