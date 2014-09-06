package com.secretapp.backend.data.models

import com.secretapp.backend.data.message.struct.{AvatarImage => AvatarImageStruct}

case class AvatarImage(fileLocation: FileLocation, width: Int, height: Int) {
  lazy val toStruct = AvatarImageStruct(fileLocation.toStruct, width, height)
}
