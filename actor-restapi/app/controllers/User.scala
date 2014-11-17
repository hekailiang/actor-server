package controllers

import errors.NotFoundException.getOrNotFound
import play.api._
import play.api.libs.json.Json
import play.api.mvc._
import play.api.libs.concurrent.Execution.Implicits._
import models.json._
import com.secretapp.backend.{persist => p}
import utils.DbConnector.Implicits._

object User extends Controller {

  def create = Action.async(parse.json) { req =>
    p.User.insertEntityWithChildren(req.body.as[models.UserCreationRequest].toUser) map { u =>
      Created(Json toJson u)
    }
  }

  def delete(id: Int) = Action.async {
    p.User.dropEntity(id) map { _ =>
      NoContent
    }
  }

  def update(id: Int) = Action.async(parse.json) { req =>
    getOrNotFound(p.User.getEntity(id)) flatMap { u =>
      p.User.insertEntityWithChildren(req.body.as[models.UserUpdateRequest] update u) map { u =>
        Ok(Json toJson u)
      }
    }
  }

  def get(id: Int) = Action.async { req =>
    getOrNotFound(p.User.getEntity(id)) map { u =>
      Ok(Json toJson u)
    }
  }

  def list(startId: Option[Int], count: Int) = Action.async {
    p.User.list(startId, utils.Pagination.fixCount(count)) map { us =>
      Ok(Json toJson us)
    }
  }

}
