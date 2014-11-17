package controllers

import play.api._
import play.api.libs.json.Json
import play.api.mvc._
import play.api.libs.concurrent.Execution.Implicits._
import models.json._

object User extends Controller {

  def create = Action.async(parse.json) { req =>
    persist.User.create(req.body.as[models.UserCreationRequest].toUser) map { u =>
      Created(Json toJson u)
    }
  }

  def delete(id: Int) = Action.async {
    persist.User.remove(id) map { _ =>
      NoContent
    }
  }

  def update(id: Int) = Action.async(parse.json) { req =>
    persist.User.getById(id) flatMap { u =>
      persist.User.save(req.body.as[models.UserUpdateRequest] update u) map { u =>
        Ok(Json toJson u)
      }
    }
  }

  def get(id: Int) = Action.async { req =>
    persist.User.getById(id) map { u =>
      Ok(Json toJson u)
    }
  }

  def list(startId: Option[Int], count: Int) = Action.async {
    persist.User.list(startId, utils.Pagination.fixCount(count)) map { us =>
      Ok(Json toJson us)
    }
  }

}
