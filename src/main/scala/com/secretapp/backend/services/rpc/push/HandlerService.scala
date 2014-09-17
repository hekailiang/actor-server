package com.secretapp.backend.services.rpc.push

import com.secretapp.backend.data.message.rpc.{Ok, ResponseVoid, RpcResponse}
import com.secretapp.backend.data.models.GooglePushCredentials
import com.secretapp.backend.persist.GooglePushCredentialsRecord
import scala.concurrent.Future

trait HandlerService {
  this: Handler =>

  import context.dispatcher

  protected def handleRequestRegisterGooglePush(projectId: Long, regId: String): Future[RpcResponse] =
    GooglePushCredentialsRecord.set(GooglePushCredentials(currentUser.uid, currentUser.authId, projectId, regId)) map { _ =>
      Ok(ResponseVoid())
    }

  protected def handleRequestUnregisterPush: Future[RpcResponse] =
    GooglePushCredentialsRecord.remove(currentUser.uid, currentUser.authId) map { _ =>
      Ok(ResponseVoid())
    }
}
