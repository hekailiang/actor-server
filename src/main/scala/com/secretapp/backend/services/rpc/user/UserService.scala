package com.secretapp.backend.services.rpc.user

import akka.pattern.ask
import com.secretapp.backend.api.ApiHandlerActor
import com.secretapp.backend.api.counters.CounterProtocol
import com.secretapp.backend.data.message.rpc.RpcRequestMessage
import com.secretapp.backend.data.message.rpc.file.FileLocation
import com.secretapp.backend.data.message.rpc.user.{RequestSetAvatar, ResponseAvatarUploaded}
import com.secretapp.backend.data.message.struct.{Avatar, AvatarImage}
import com.secretapp.backend.data.message.update.AvatarChanged
import com.secretapp.backend.data.models.User
import com.secretapp.backend.data.transport.Package
import com.secretapp.backend.persist.{FileRecord, UserRecord}
import com.secretapp.backend.services.GeneratorService
import com.secretapp.backend.services.common.PackageCommon
import com.secretapp.backend.services.rpc.RpcCommon
import com.secretapp.backend.services.rpc.files.FilesService
import com.sksamuel.scrimage.{AsyncImage, Format, Position}

import scala.util.Random
import scalaz.Scalaz._
import scalaz._

trait UserService extends PackageCommon with RpcCommon with FilesService {
  self: ApiHandlerActor with GeneratorService =>

  import context._

  def handleRpcUser(p: Package, messageId: Long): PartialFunction[RpcRequestMessage, Unit] = {
    case r: RequestSetAvatar => authorizedRequest(p)(u => sendRpcResult(p, messageId)(handleSetAvatar(u, p, r)))
  }

  private def handleSetAvatar(u: User, p: Package, r: RequestSetAvatar): RpcResult = {
    val fr = new FileRecord

    for (
      fullImageBytes  <- fr.getFile(r.fileLocation.fileId.toInt);

      fullImage       <- AsyncImage(fullImageBytes);
      smallImage      <- fullImage.resizeTo(100, 100, Position.Center);
      largeImage      <- fullImage.resizeTo(200, 200, Position.Center);

      smallImageId    <- ask(clusterProxies.filesCounter, CounterProtocol.GetNext).mapTo[CounterProtocol.StateType];
      largeImageId    <- ask(clusterProxies.filesCounter, CounterProtocol.GetNext).mapTo[CounterProtocol.StateType];

      _               <- fr.createFile(smallImageId, (new Random).nextString(30)); // TODO: genAccessSalt makes specs
      _               <- fr.createFile(largeImageId, (new Random).nextString(30)); // fail

      smallImageHash  <- fr.getAccessHash(smallImageId);
      largeImageHash  <- fr.getAccessHash(largeImageId);

      smallImageLoc    = FileLocation(smallImageId, smallImageHash);
      largeImageLoc    = FileLocation(largeImageId, largeImageHash);

      smallImageBytes <- smallImage.writer(Format.JPEG).write();
      largeImageBytes <- largeImage.writer(Format.JPEG).write();

      _               <- fr.write(smallImageLoc.fileId.toInt, 0, smallImageBytes);
      _               <- fr.write(largeImageLoc.fileId.toInt, 0, largeImageBytes);

      smallAvatarImage = AvatarImage(smallImageLoc, 100, 100);
      largeAvatarImage = AvatarImage(largeImageLoc, 200, 200);
      fullAvatarImage  = AvatarImage(r.fileLocation, fullImage.width, fullImage.height);

      avatar           = Avatar(smallAvatarImage.some, largeAvatarImage.some, fullAvatarImage.some);

      _               <- UserRecord.updateAvatar(u.authId, u.uid, avatar)

    ) yield {
      sendUpdates(u, avatar)
      ResponseAvatarUploaded(avatar).right
    }
  }

  private def sendUpdates(u: User, avatar: Avatar): Unit = withRelations(u.uid) {
    _.foreach(pushUpdate(_, AvatarChanged(u.uid, avatar.some)))
  }
}
