package com.secretapp.backend.services.rpc

import com.secretapp.backend.api.ApiHandlerActor
import com.secretapp.backend.data.message.rpc.RpcRequestMessage
import com.secretapp.backend.data.message.rpc.user.{ResponseAvatarUploaded, RequestSetAvatar}
import com.secretapp.backend.data.models.{FileLocation, AvatarImage, Avatar}
import com.secretapp.backend.data.transport.Package
import com.secretapp.backend.persist.FileRecord
import com.secretapp.backend.services.GeneratorService
import com.secretapp.backend.services.common.PackageCommon
import com.secretapp.backend.services.rpc.files.FilesService
import com.sksamuel.scrimage.{Position, Format, AsyncImage}
import scalaz._
import Scalaz._

trait ProfileService extends PackageCommon with RpcCommon with FilesService {
  self: ApiHandlerActor with GeneratorService =>

  import context._

  def handleRpcProfile(p: Package, messageId: Long): PartialFunction[RpcRequestMessage, Unit] = {
    case r: RequestSetAvatar => sendRpcResult(p, messageId)(handleSetAvatar(p, r))
  }

  private def handleSetAvatar(p: Package, r: RequestSetAvatar): RpcResult = {
    val fr = new FileRecord

    for (
      fullImageBytes  <- fr.getFile(r.fileLocation.fileId);

      fullImage       <- AsyncImage(fullImageBytes);
      smallImage      <- fullImage.resizeTo(100, 100, Position.Center);
      largeImage      <- fullImage.resizeTo(200, 200, Position.Center);

      smallImageId     = genFileId;
      largeImageId     = genFileId;

      smallImageBytes <- smallImage.writer(Format.JPEG).write();
      largeImageBytes <- largeImage.writer(Format.JPEG).write();

      _               <- fr.createFile(smallImageId, genFileAccessSalt);
      _               <- fr.createFile(largeImageId, genFileAccessSalt);

      _               <- fr.write(smallImageId, 0, smallImageBytes);
      _               <- fr.write(largeImageId, 0, largeImageBytes);

      smallAvatarImage = AvatarImage(FileLocation(smallImageId), 100, 100);
      largeAvatarImage = AvatarImage(FileLocation(largeImageId), 200, 200);
      fullAvatarImage  = AvatarImage(FileLocation(smallImageId), fullImage.width, fullImage.height);

      avatar           = Avatar(smallAvatarImage.some, largeAvatarImage.some, fullAvatarImage.some)

    ) yield ResponseAvatarUploaded(avatar.toStruct).right
  }
}
