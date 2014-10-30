package com.secretapp.backend.util

import akka.actor._
import akka.pattern.ask
import akka.util.Timeout
import com.secretapp.backend.api.counters.CounterProtocol
import com.secretapp.backend.services.common.RandomService
import com.sksamuel.scrimage.{ AsyncImage, Format, Position }
import com.secretapp.backend.models
import com.secretapp.backend.persist.FileRecord
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

  def scaleAvatar(fr: FileRecord, fc: ActorRef, fl: models.FileLocation)
                 (implicit ec: ExecutionContext, timeout: Timeout): Future[models.Avatar] = {
    for (
      fullImageBytes   <- fr.getFile(fl.fileId.toInt);
      (fiw, fih)       <- dimensions(fullImageBytes);

      smallImageId     <- ask(fc, CounterProtocol.GetNext).mapTo[CounterProtocol.StateType];
      largeImageId     <- ask(fc, CounterProtocol.GetNext).mapTo[CounterProtocol.StateType];

      _                <- fr.createFile(smallImageId, rand.nextString(30)); // TODO: genAccessSalt makes specs
      _                <- fr.createFile(largeImageId, rand.nextString(30)); // fail

      smallImageHash   <- fr.getAccessHash(smallImageId);
      largeImageHash   <- fr.getAccessHash(largeImageId);

      smallImageLoc    = models.FileLocation(smallImageId, smallImageHash);
      largeImageLoc    = models.FileLocation(largeImageId, largeImageHash);

      smallImageBytes <- AvatarUtils.resizeToSmall(fullImageBytes);
      largeImageBytes <- AvatarUtils.resizeToLarge(fullImageBytes);

      _               <- fr.write(smallImageLoc.fileId.toInt, 0, smallImageBytes);
      _               <- fr.write(largeImageLoc.fileId.toInt, 0, largeImageBytes);

      smallAvatarImage = models.AvatarImage(smallImageLoc, 100, 100, smallImageBytes.length);
      largeAvatarImage = models.AvatarImage(largeImageLoc, 200, 200, largeImageBytes.length);
      fullAvatarImage  = models.AvatarImage(fl, fiw, fih, fullImageBytes.length);

      avatar           = models.Avatar(smallAvatarImage.some, largeAvatarImage.some, fullAvatarImage.some)

    ) yield avatar
  }
}
