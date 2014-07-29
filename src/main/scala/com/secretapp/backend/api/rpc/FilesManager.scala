package com.secretapp.backend.api.rpc

import akka.actor._
import akka.pattern.ask
import akka.util.Timeout
import com.datastax.driver.core.{ Session => CSession }
import com.secretapp.backend.api.{ CounterActor, CounterProtocol, SharedActors }
import com.secretapp.backend.data.message.RpcResponseBox
import com.secretapp.backend.data.message.rpc.Ok
import com.secretapp.backend.data.message.rpc.file._
import com.secretapp.backend.data.models.User
import com.secretapp.backend.data.transport.Package
import com.secretapp.backend.persist.FileBlockRecord
import com.secretapp.backend.services.common.PackageCommon._
import scala.concurrent.Future
import scala.concurrent.duration._
import scala.util.{ Failure, Success }
import scalaz._
import scalaz.Scalaz._
import scodec.bits._
import scodec.codecs.{ int32 => int32codec }

class FilesManager(val handleActor: ActorRef, val currentUser: User, val fileBlockRecord: FileBlockRecord)(implicit val session: CSession) extends Actor with ActorLogging with FilesService {
  import context.system

  implicit val timeout = Timeout(5.seconds)

  def fileCounter: Future[ActorRef] = {
    SharedActors.lookup("file-counter") {
      system.actorOf(Props(new CounterActor("file")), "file-counter")
    }
  }

  def receive = {
    case RpcProtocol.Request(p, messageId,
      RequestUploadStart()) =>
      handleRequestUploadStart(p, messageId)()
    case RpcProtocol.Request(p, messageId, RequestUploadFile(config, offset, data)) =>
      handleRequestUploadFile(p, messageId)(config, offset, data)
  }
}

trait FilesService {
  self: FilesManager =>

  import context.dispatcher

  protected def handleRequestUploadStart(p: Package, messageId: Long)() = {
    for {
      fileId <- fileCounter.flatMap(ask(_, CounterProtocol.GetNext).mapTo[CounterProtocol.StateType])
    } yield {
      val rsp = ResponseUploadStart(UploadConfig(int32codec.encodeValid(fileId)))
      handleActor ! PackageToSend(p.replyWith(messageId, RpcResponseBox(messageId, Ok(rsp))).right)
    }
  }

  protected def handleRequestUploadFile(p: Package, messageId: Long)(config: UploadConfig, offset: Int, data: BitVector) = {
    // TODO: handle failures
    val fileId = int32codec.decodeValidValue(config.serverData)
    fileBlockRecord.write(fileId, offset, data.toByteArray) onComplete {
      case Success(_) =>

      case Failure(e) =>
        log.error("Failed to upload file chunk {} {} {}", p, messageId, e)
    }

    val rsp = ResponseFileUploadStarted()
    handleActor ! PackageToSend(p.replyWith(messageId, RpcResponseBox(messageId, Ok(rsp))).right)
  }
}
