package com.secretapp.backend.api

import com.secretapp.backend.data.message.rpc.messaging._
import com.secretapp.backend.data.message.struct
import com.secretapp.backend.models
import com.secretapp.backend.services.rpc.RpcSpec
import com.secretapp.backend.util.ACL
import scala.collection.immutable
import scodec.bits._

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
      message = message
    ) :~> <~:[ResponseSeqDate]

    (resp, randomId)
  }

  protected def sendEncryptedMessage(
    user: models.User,
    encryptedMessage: BitVector = BitVector(1, 2, 3)
  )(implicit scope: TestScope) = {
    val rq = RequestSendEncryptedMessage(
      struct.OutPeer.privat(user.uid, ACL.userAccessHash(scope.user.authId, user)),
      randomId = rand.nextLong,
      encryptedMessage = encryptedMessage,
      keys = immutable.Seq(
        EncryptedAESKey(
          user.publicKeyHash, BitVector(1, 0, 1, 0)
        )
      ),
      ownKeys = immutable.Seq(
        EncryptedAESKey(
          scope.user.publicKeyHash, BitVector(1, 0, 1, 0)
        )
      )
    )

    val (resp, _) = rq :~> <~:[ResponseSeqDate]
    resp
  }
}
