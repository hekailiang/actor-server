package im.actor.server.rest.controllers

import im.actor.server.persist.file.adapter.fs.FileStorageAdapter
import spray.json._
import im.actor.server.rest.models._
import akka.http.marshallers.sprayjson.SprayJsonSupport._
import akka.http.server.Directives._
import akka.http.model._
import com.secretapp.backend.models
import com.secretapp.backend.persist
import im.actor.server.rest.utils.JsonImplicits._
import akka.http.server.RequestContext
import scala.concurrent.{ ExecutionContext, Future }

object UsersController {
  def index(ctx: RequestContext)(implicit ec: ExecutionContext) = ctx.complete {
    for {(users, totalCount) <- UserItem.paginate(ctx.request.uri.query.toMap)}
    yield toJson(users, totalCount)
  }

  def show(userId: Int)(implicit ec: ExecutionContext) = complete {
    for {userOpt <- UserItem.show(userId)}
    yield userOpt match {
      case Some(user) => user.toJson.asJsObject
      case None => JsonErrors.NotFound
    }
  }

  def avatar(userId: Int, size: String, fileAdapter: FileStorageAdapter)(implicit ec: ExecutionContext) = complete {
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
      case Some(fileData) => HttpResponse(entity = HttpEntity(MediaTypes.`image/png`, fileData)) // TODO: content type
      case _ => HttpResponse(
        status = StatusCodes.SeeOther,
        headers = headers.Location("/assets/images/avatar.png") :: Nil,
        entity = HttpEntity.Empty
      )
    }
  }
}
