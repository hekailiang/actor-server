package com.secretapp.backend.util

import akka.actor._
import akka.pattern.ask
import akka.util.Timeout
import com.secretapp.backend.api.counters.CounterProtocol
import com.secretapp.backend.services.common.RandomService
import com.sksamuel.scrimage.{ AsyncImage, Format, Position }
import com.secretapp.backend.models
import com.secretapp.backend.persist
import im.actor.server.persist.file.adapter.FileAdapter
import scala.concurrent.{ ExecutionContext, Future }
import scalaz._
import Scalaz._

object AvatarUtils extends RandomService {

  private def resizeTo(imgBytes: Array[Byte], side: Int)
    (implicit ec: ExecutionContext): Future[Array[Byte]] =
    for (
      img          <- AsyncImage(imgBytes);
      scaleCoef     = side.toDouble / math.min(img.width, img.height);
      scaledImg    <- img.scale(scaleCoef);
      resizedImg   <- scaledImg.resizeTo(side, side, Position.Center);
      resizedBytes <- resizedImg.writer(Format.JPEG).write()
    ) yield resizedBytes

  def resizeToSmall(imgBytes: Array[Byte])
    (implicit ec: ExecutionContext) =
    resizeTo(imgBytes, 100)

  def resizeToLarge(imgBytes: Array[Byte])
    (implicit ec: ExecutionContext) =
    resizeTo(imgBytes, 200)

  def dimensions(imgBytes: Array[Byte])
    (implicit ec: ExecutionContext): Future[(Int, Int)] =
    AsyncImage(imgBytes) map { i => (i.width, i.height) }

  // TODO: get rid of Option.get
  def scaleAvatar(fa: FileAdapter, fl: models.FileLocation)
    (implicit ec: ExecutionContext, timeout: Timeout, s: ActorSystem): Future[models.Avatar] = {
    val smallImageId = rand.nextLong
    val largeImageId = rand.nextLong

    for {
      fullImageFD      <- persist.FileData.find(fl.fileId)

      fullImageBytes   <- fa.readAll(fullImageFD.get.adapterData)
      (fiw, fih)       <- dimensions(fullImageBytes)

      _                <- persist.File.create(fa, smallImageId, rand.nextString(30)) // TODO: genAccessSalt makes specs
      _                <- persist.File.create(fa, largeImageId, rand.nextString(30)) // fail

      smallImageFD     <- persist.FileData.find(smallImageId)
      largeImageFD     <- persist.FileData.find(largeImageId)

      // FIXME: make it safe (Option.get)
      smallImageLoc    = models.FileLocation(smallImageId, ACL.fileAccessHash(smallImageId, smallImageFD.get.accessSalt))
      largeImageLoc    = models.FileLocation(largeImageId, ACL.fileAccessHash(largeImageId, largeImageFD.get.accessSalt))

      smallImageBytes <- AvatarUtils.resizeToSmall(fullImageBytes)
      largeImageBytes <- AvatarUtils.resizeToLarge(fullImageBytes)

      _               <- persist.File.write(fa, smallImageLoc.fileId, 0, smallImageBytes)
      _               <- persist.File.write(fa, largeImageLoc.fileId, 0, largeImageBytes)

      smallAvatarImage = models.AvatarImage(smallImageLoc, 100, 100, smallImageBytes.length)
      largeAvatarImage = models.AvatarImage(largeImageLoc, 200, 200, largeImageBytes.length)
      fullAvatarImage  = models.AvatarImage(fl, fiw, fih, fullImageBytes.length)

      avatar           = models.Avatar(smallAvatarImage.some, largeAvatarImage.some, fullAvatarImage.some)

    } yield avatar
  }
}
