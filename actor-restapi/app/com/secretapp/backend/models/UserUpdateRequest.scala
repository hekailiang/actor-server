package com.secretapp.backend.models

import scodec.bits.BitVector
import utils.OptSet._
import scalaz._
import Scalaz._

case class UserUpdateRequest(
  publicKey:   Option[BitVector],
  phoneNumber: Option[Long],
  name:        Option[String],
  sex:         Option[Sex],
  avatar:      Option[Avatar]
) {

  def update(u: User): User =
    u
      .optSet(publicKey  )((u, v) => u.copy(publicKey   = v))
      .optSet(phoneNumber)((u, v) => u.copy(phoneNumber = v))
      .optSet(name       )((u, v) => u.copy(name        = v))
      .optSet(sex        )((u, v) => u.copy(sex         = v))
      .optSet(avatar     )((u, v) => u.copy(
        smallAvatarFileId   = v.smallImage.map(_.fileLocation.fileId.toInt),
        smallAvatarFileHash = v.smallImage.map(_.fileLocation.accessHash),
        smallAvatarFileSize = v.smallImage.map(_.fileSize),
        largeAvatarFileId   = v.largeImage.map(_.fileLocation.fileId.toInt),
        largeAvatarFileHash = v.largeImage.map(_.fileLocation.accessHash),
        largeAvatarFileSize = v.largeImage.map(_.fileSize),
        fullAvatarFileId    = v.fullImage.map(_.fileLocation.fileId.toInt),
        fullAvatarFileHash  = v.fullImage.map(_.fileLocation.accessHash),
        fullAvatarFileSize  = v.fullImage.map(_.fileSize),
        fullAvatarWidth     = v.fullImage.map(_.width),
        fullAvatarHeight    = v.fullImage.map(_.height)
      ))

}
