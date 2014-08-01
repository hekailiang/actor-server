package com.secretapp.backend.api.rpc

import akka.actor._
import akka.cluster.ClusterEvent._
import akka.contrib.pattern.ClusterSingletonProxy
import akka.pattern.ask
import akka.util.Timeout
import com.datastax.driver.core.{ Session => CSession }
import com.secretapp.backend.api.counters.CounterProtocol
import com.secretapp.backend.data.message.RpcResponseBox
import com.secretapp.backend.data.message.rpc.{ Error, Ok }
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
  import context.dispatcher

  implicit val timeout = Timeout(5.seconds)

  var leaderAddress: Option[Address] = None

  val filesCounter = system.actorOf(
    ClusterSingletonProxy.props(
      singletonPath = "/user/singleton/files-counter",
      role = None),
    name = "files-counter-proxy")

  def receive = {
    case rq @ RpcProtocol.Request(p, messageId, RequestUploadStart()) =>
      ask(filesCounter, CounterProtocol.GetNext).mapTo[CounterProtocol.StateType] onComplete {
        case Success(fileId) =>
          val rsp = ResponseUploadStart(UploadConfig(int32codec.encodeValid(fileId)))
          handleActor ! PackageToSend(p.replyWith(messageId, RpcResponseBox(messageId, Ok(rsp))).right)
        case Failure(e) =>
          val msg = s"Failed to get next valie if file id sequence: ${e.getMessage}"
          handleActor ! PackageToSend(
            p.replyWith(messageId, RpcResponseBox(messageId, Error(400, "INTERNAL_ERROR", msg, true))).right)
          log.error(msg)
      }
    case RpcProtocol.Request(p, messageId, RequestUploadFile(config, offset, data)) =>
      handleRequestUploadFile(p, messageId)(config, offset, data)
  }
}

trait FilesService {
  self: FilesManager =>

  import context.dispatcher

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
