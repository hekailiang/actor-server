package controllers

import com.secretapp.backend.models
import errors.NotFoundException.getOrNotFound
import play.api._
import play.api.libs.json.Json
import play.api.mvc._
import play.api.libs.concurrent.Execution.Implicits._
import json._
import com.secretapp.backend.persist
import utils.DbConnector.Implicits._

object User extends Controller {

  def create = Action.async(parse.json) { req =>
    persist.User.insertEntityWithChildren(req.body.as[models.UserCreationRequest].toUser) map { u =>
      Created(Json toJson u)
    }
  }

  def delete(id: Int) = Action.async {
    persist.User.dropEntity(id) map { _ =>
      NoContent
    }
  }

  def update(id: Int) = Action.async(parse.json) { req =>
    getOrNotFound(persist.User.getEntity(id)) flatMap { u =>
      persist.User.insertEntityWithChildren(req.body.as[models.UserUpdateRequest] update u) map { u =>
        Ok(Json toJson u)
      }
    }
  }

  def get(id: Int) = Action.async { req =>
    getOrNotFound(persist.User.getEntity(id)) map { u =>
      Ok(Json toJson u)
    }
  }

  def list(startId: Option[Int], count: Int) = Action.async {
    persist.User.list(startId, utils.Pagination.fixCount(count)) map { us =>
      Ok(Json toJson us)
    }
  }

}
