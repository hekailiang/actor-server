package com.secretapp.backend.data.models

import com.secretapp.backend.data.message.rpc.file.{FileLocation => FileLocationStruct}

case class FileLocation(fileId: Int) {
  lazy val toStruct = FileLocationStruct(fileId, 0)
}
