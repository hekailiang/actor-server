package com.secretapp.backend.services.rpc.files

import akka.actor._
import akka.pattern.ask
import com.secretapp.backend.api.ApiBrokerService
import com.secretapp.backend.api.rpc.RpcProtocol
import com.secretapp.backend.data.message.rpc.{ RpcRequestMessage, RpcResponse }
import com.secretapp.backend.data.message.rpc.file._
import scala.concurrent.Future
import scalaz._
import Scalaz._

trait FilesService {
  this: ApiBrokerService =>

  lazy val filesHandler = context.actorOf(Props(new Handler(currentUser.get, fileRecord, clusterProxies.filesCounterProxy)), "files")

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
