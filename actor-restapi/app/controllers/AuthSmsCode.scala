package controllers

import com.secretapp.backend.models
import play.api._
import play.api.libs.json.Json
import play.api.mvc._
import play.api.libs.concurrent.Execution.Implicits._
import scalaz._
import Scalaz._
import json._
import errors.NotFoundException.getOrNotFound
import utils.DbConnector.Implicits._
import com.secretapp.backend.persist

object AuthSmsCode extends Controller {

  def delete(rawPhone: Long) = Action.async {
    persist.AuthSmsCode.dropEntity(validPhone(rawPhone)) map { _ =>
      NoContent
    }
  }

  def update(rawPhone: Long) = Action.async(parse.json) { req =>
    val phone = validPhone(rawPhone)
    persist.AuthSmsCode.getEntity(phone) flatMap {
      case Some(c) =>
        persist.AuthSmsCode.insertEntity(req.body.as[models.AuthSmsCodeUpdateRequest] update c) map { c =>
          Ok(Json toJson c)
        }
      case None    =>
        persist.AuthSmsCode.insertEntity(req.body.as[models.AuthSmsCodeCreationRequest] toAuthSmsCode phone) map { c =>
          Created(Json toJson c)
        }
    }
  }

  def get(phoneNumber: Long) = Action.async { req =>
    getOrNotFound(persist.AuthSmsCode.getEntity(validPhone(phoneNumber))) map { c =>
      Ok(Json toJson c)
    }
  }

  def list(startPhone: Option[Long], count: Int) = Action.async { req =>
    persist.AuthSmsCode.list(startPhone.map(validPhone), utils.Pagination.fixCount(count))
      .map { cs => Ok(Json toJson cs) }
  }

  private def validPhone(phone: Long): Long =
    if (phone.toString.length == 11)
      phone
    else
      throw new errors.BadRequestException("phone is invalid")

}
