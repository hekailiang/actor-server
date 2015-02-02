package controllers

import _root_.models._
import com.secretapp.backend.models
import com.secretapp.backend.persist
import im.actor.server.persist.file.adapter.fs.FileStorageAdapter
import utils.JsonImplicits._
import play.api.mvc._
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.libs.concurrent.Akka
import play.api.libs.json.Json
import play.api.Play.current
import scala.concurrent.Future

object UsersController extends Controller {
  lazy val fileAdapter = new FileStorageAdapter(Akka.system)

  def index() = Action.async { req =>
    for { (users, totalCount) <- UserItem.paginate(req.queryString) }
    yield Ok(toJson(users, totalCount))
  }

  def show(userId: Int) = Action.async { req =>
    for { userOpt <- UserItem.show(userId) }
    yield userOpt match {
      case Some(user) => Ok(Json.toJson(user))
      case None => NotFound
    }
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
          case Some(avatarImage) => persist.File.readAll(fileAdapter, avatarImage.fileLocation.fileId).map(Some(_))
          case _ => Future.successful(None)
        }
      case _ => Future.successful(None)
    }.map {
      case Some(fileData) => Ok(fileData).as("image/png") // TODO: content type
      case _ => Redirect("/assets/images/avatar.png")
    }
  }
}
