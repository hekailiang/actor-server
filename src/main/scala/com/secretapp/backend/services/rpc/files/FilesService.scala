package com.secretapp.backend.services.rpc.files

import akka.actor._
import akka.pattern.ask
import com.secretapp.backend.api.{ ApiBrokerService }
import com.secretapp.backend.api.rpc.RpcProtocol
import com.secretapp.backend.data.message.rpc.{ RpcRequestMessage, RpcResponse }
import com.secretapp.backend.data.message.rpc.file._
import com.secretapp.backend.data.models.User
import com.secretapp.backend.data.transport.Package
import com.secretapp.backend.persist.FileRecord
import com.secretapp.backend.protocol.transport._
import scala.concurrent.Future
import scalaz._
import Scalaz._

trait FilesService {
  this: ApiBrokerService =>

  import context.dispatcher
  import context.system

  lazy val filesHandler = context.actorOf(Props(new Handler(currentUser.get, new FileRecord, clusterProxies.filesCounter)), "files")

  def handleRpcFiles: PartialFunction[RpcRequestMessage, \/[Throwable, Future[RpcResponse]]] = {
    case rq: RequestStartUpload =>
      authorizedRequest {
        (filesHandler ? RpcProtocol.Request(rq)).mapTo[RpcResponse]
      }
    case rq: RequestUploadPart =>
      authorizedRequest {
        (filesHandler ? RpcProtocol.Request(rq)).mapTo[RpcResponse]
      }
    case rq: RequestCompleteUpload =>
      authorizedRequest {
        (filesHandler ? RpcProtocol.Request(rq)).mapTo[RpcResponse]
      }
    case rq: RequestGetFile =>
      authorizedRequest {
        (filesHandler ? RpcProtocol.Request(rq)).mapTo[RpcResponse]
      }
  }
}
