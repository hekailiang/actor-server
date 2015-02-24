package com.secretapp.backend.helpers

import akka.actor._
import akka.event.LoggingAdapter
import akka.util.Timeout
import com.secretapp.backend.data.message.rpc.{ RpcResponse, Error}
import com.secretapp.backend.models
import com.secretapp.backend.persist
import com.secretapp.backend.util.AvatarUtils
import im.actor.server.persist.file.adapter.FileAdapter
import scala.concurrent._

trait GroupAvatarHelpers {
  val context: ActorContext
  val fileAdapter: FileAdapter

  import context.dispatcher
  implicit val timeout: Timeout

  def log: LoggingAdapter

  def withValidAvatar(fl: models.FileLocation)(f: => Future[RpcResponse]): Future[RpcResponse] =
    persist.FileData.find(fl.fileId) flatMap {
      case Some(models.FileData(_, _, len, _)) =>
        val sizeLimit: Long = 1024 * 1024 // TODO: configurable

        if (len > sizeLimit)
          Future successful Error(400, "FILE_TOO_BIG", "", false)
        else
          f
      case None =>
        Future successful Error(400, "LOCATION_INVALID", "", false)
    }

  def withScaledAvatar(fl: models.FileLocation)
                      (f: models.Avatar => Future[RpcResponse])
                      (implicit ec: ExecutionContext, timeout: Timeout, s: ActorSystem): Future[RpcResponse] =
    AvatarUtils.scaleAvatar(fileAdapter, fl) flatMap f recover {
      case e =>
        log.warning(s"Failed while updating avatar: $e")
        Error(400, "IMAGE_LOAD_ERROR", "", false)
    }

  def withValidScaledAvatar(fl: models.FileLocation)
                           (f: models.Avatar => Future[RpcResponse])
                           (implicit ec: ExecutionContext, timeout: Timeout, s: ActorSystem): Future[RpcResponse] =
    withValidAvatar(fl) {
      withScaledAvatar(fl)(f)
    }
}
