package com.secretapp.backend.services.rpc.push

import com.secretapp.backend.data.message.rpc.{ Ok, ResponseVoid, RpcResponse }
import com.secretapp.backend.models
import com.secretapp.backend.persist
import scala.concurrent.Future

trait HandlerService {
  this: Handler =>

  import context.dispatcher

  protected def handleRequestRegisterGooglePush(projectId: Long, regId: String): Future[RpcResponse] =
    persist.GooglePushCredentials.set(models.GooglePushCredentials(currentAuthId, projectId, regId)) map { _ =>
      Ok(ResponseVoid())
    }

  protected def handleRequestRegisterApplePush(apnsKey: Int, token: String): Future[RpcResponse] =
    persist.ApplePushCredentials.set(models.ApplePushCredentials(currentAuthId, apnsKey, token)) map { _ =>
      Ok(ResponseVoid())
    }

  protected def handleRequestUnregisterPush: Future[RpcResponse] =
    Future.sequence(Seq(
      persist.GooglePushCredentials.remove(currentAuthId),
      persist.ApplePushCredentials.remove(currentAuthId)
    )) map { _ =>
      Ok(ResponseVoid())
    }
}
