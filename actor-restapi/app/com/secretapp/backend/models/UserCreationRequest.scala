package com.secretapp.backend.models

import scodec.bits.BitVector
import utils.PublicKey
import scala.util.Random
import scala.collection.immutable

case class UserCreationRequest(
  publicKey:   BitVector,
  phoneNumber: Long,
  name:        String,
  sex:         Sex,
  avatar:      Option[Avatar] = None
) {

  def toUser: User = {
    val publicKeyHash = PublicKey.keyHash(publicKey)

    User(
      0, // Will be generated at persist-time anyway.
      Random.nextLong(),
      publicKeyHash,
      publicKey,
      phoneNumber,
      Random.nextString(30),
      name,
      "", // TODO:
      sex,
      avatar.flatMap(_.smallImage.map(_.fileLocation.fileId.toInt)),
      avatar.flatMap(_.smallImage.map(_.fileLocation.accessHash)),
      avatar.flatMap(_.smallImage.map(_.fileSize)),
      avatar.flatMap(_.largeImage.map(_.fileLocation.fileId.toInt)),
      avatar.flatMap(_.largeImage.map(_.fileLocation.accessHash)),
      avatar.flatMap(_.largeImage.map(_.fileSize)),
      avatar.flatMap(_.fullImage.map(_.fileLocation.fileId.toInt)),
      avatar.flatMap(_.fullImage.map(_.fileLocation.accessHash)),
      avatar.flatMap(_.fullImage.map(_.fileSize)),
      avatar.flatMap(_.fullImage.map(_.width)),
      avatar.flatMap(_.fullImage.map(_.height)),
      immutable.Set(publicKeyHash)
    )
  }

}
