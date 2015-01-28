package controllers

import _root_.models._
import com.secretapp.backend.models
import com.secretapp.backend.persist
import utils.JsonImplicits._
import play.api.mvc._
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import scala.concurrent.Future

object UsersController extends Controller {
  def index() = Action.async { req =>
    for { (users, totalCount) <- UserItem.paginate(req.queryString) }
    yield Ok(toJson(users, totalCount))
  }

  def show(userId: Int) = Action.async { req =>
    scala.concurrent.Future.successful(Ok("nothing"))
  }

  def avatar(userId: Int, size: String) = Action.async { req =>
    persist.AvatarData.find(id = userId, typ = persist.AvatarData.typeVal[models.User]).flatMap {
      case Some(avatarData) =>
        val imageOpt = size match {
          case "full" => avatarData.fullAvatarImage
          case "large" => avatarData.largeAvatarImage
          case "small" => avatarData.smallAvatarImage
          case _ => None
        }
        imageOpt match {
          case Some(avatarImage) => ??? // persist.File.find(avatarImage.fileLocation.fileId)
          case _ => Future.successful(None)
        }
      case _ => Future.successful(None)
    }.map {
//      case Some(fileData) => Ok("TODO")
      case _ => Redirect("/assets/images/avatar.png")
    }
  }
}
