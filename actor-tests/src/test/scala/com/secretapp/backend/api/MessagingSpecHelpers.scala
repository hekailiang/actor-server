package com.secretapp.backend.api

import com.secretapp.backend.data.message.rpc.messaging._
import com.secretapp.backend.data.message.struct
import com.secretapp.backend.models
import com.secretapp.backend.services.rpc.RpcSpec
import com.secretapp.backend.util.ACL
import scala.collection.immutable

trait MessagingSpecHelpers {
  this: RpcSpec =>

  protected def sendMessage(
    user: models.User,
    message: MessageContent = TextMessage("Yolo!")
  )(implicit scope: TestScope): (ResponseSeqDate, Long) = {

    val randomId = rand.nextLong

    val (resp, _) = RequestSendMessage(
      outPeer = struct.OutPeer.privat(user.uid, ACL.userAccessHash(scope.user.authId, user)),
      randomId = randomId,
      message = TextMessage("Yolo!")
    ) :~> <~:[ResponseSeqDate]

    (resp, randomId)
  }
}
