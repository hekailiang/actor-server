package controllers

import models.AuthLogItem
import utils.JsonImplicits._
import play.api.mvc._
import play.api.libs.concurrent.Execution.Implicits.defaultContext

object AuthLogsController extends Controller {

  def index() = Action.async { req =>
    for { (logs, totalCount) <- AuthLogItem.paginate(req.queryString) }
    yield Ok(toJson(logs, totalCount))
  }

}
