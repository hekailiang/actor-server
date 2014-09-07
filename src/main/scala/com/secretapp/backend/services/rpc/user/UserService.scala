package com.secretapp.backend.services.rpc.user

import akka.pattern.ask
import com.secretapp.backend.api.ApiHandlerActor
import com.secretapp.backend.api.counters.CounterProtocol
import com.secretapp.backend.data.message.rpc.RpcRequestMessage
import com.secretapp.backend.data.message.rpc.file.FileLocation
import com.secretapp.backend.data.message.rpc.user.{RequestSetAvatar, ResponseAvatarUploaded}
import com.secretapp.backend.data.message.struct.{Avatar, AvatarImage}
import com.secretapp.backend.data.transport.Package
import com.secretapp.backend.persist.{UserRecord, FileRecord}
import com.secretapp.backend.services.GeneratorService
import com.secretapp.backend.services.common.PackageCommon
import com.secretapp.backend.services.rpc.RpcCommon
import com.secretapp.backend.services.rpc.files.FilesService
import com.sksamuel.scrimage.{Format, Position, AsyncImage}
import scala.util.Random
import scalaz._
import Scalaz._

trait UserService extends PackageCommon with RpcCommon with FilesService {
  self: ApiHandlerActor with GeneratorService =>

  import context._

  def handleRpcProfile(p: Package, messageId: Long): PartialFunction[RpcRequestMessage, Unit] = {
    case r: RequestSetAvatar => sendRpcResult(p, messageId)(handleSetAvatar(p, r))
  }

  private def handleSetAvatar(p: Package, r: RequestSetAvatar): RpcResult = {
    val fr = new FileRecord

    for (
      fullImageBytes  <- fr.getFile(r.fileLocation.fileId.toInt);

      fullImage       <- AsyncImage(fullImageBytes);
      smallImage      <- fullImage.resizeTo(100, 100, Position.Center);
      largeImage      <- fullImage.resizeTo(200, 200, Position.Center);

      smallImageId    <- ask(countersProxies.files, CounterProtocol.GetNext).mapTo[CounterProtocol.StateType];
      largeImageId    <- ask(countersProxies.files, CounterProtocol.GetNext).mapTo[CounterProtocol.StateType];

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

      _               <- UserRecord.updateAvatar(getUser.get.uid, avatar)

    ) yield ResponseAvatarUploaded(avatar).right
  }
}
