package im.actor.server.smtpd.internal

import akka.actor._
import akka.util.Timeout
import com.secretapp.backend.api.Singletons
import com.secretapp.backend.data.message.rpc.messaging.MessageContent
import com.secretapp.backend.data.message.struct
import com.secretapp.backend.helpers._
import com.secretapp.backend.models
import com.secretapp.backend.services.common.RandomService
import com.secretapp.backend.services.rpc.messaging.SendMessagingHandlers
import scala.concurrent.duration._

object ApiBrokerActor {
  def props(singletons: Singletons, updatesBrokerRegion: ActorRef, socialBrokerRegion: ActorRef) =
    Props(new ApiBrokerActor(singletons, updatesBrokerRegion, socialBrokerRegion))

  case class SendMessage(currentUser: models.User,
                         outPeer: struct.OutPeer,
                         randomId: Long,
                         message: MessageContent)

  case class AddContact(currentUser: models.User, contactUser: models.User)
}

class ApiBrokerActor(singletons: Singletons, val updatesBrokerRegion: ActorRef, socialBrokerRegion: ActorRef)
  extends Actor with ActorLogging with RandomService with UserHelpers with GroupHelpers with PeerHelpers
  with UpdatesHelpers with HistoryHelpers with SendMessagingHandlers with ContactHelpers {

  import context._
  import im.actor.server.smtpd.internal.ApiBrokerActor._

  implicit val timeout: Timeout = Timeout(5.seconds)

  val dialogManagerRegion = singletons.dialogManagerRegion

  def receive = {
    case SendMessage(currentUser, outPeer, randomId, message) =>
      sendMessage(socialBrokerRegion, singletons.dialogManagerRegion, currentUser, outPeer, randomId, message)
    case AddContact(currentUser, contactUser) =>
      // TODO
  }
}
