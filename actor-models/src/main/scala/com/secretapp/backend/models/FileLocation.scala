package com.secretapp.backend.models

@SerialVersionUID(1L)
case class FileLocation(fileId: Long, accessHash: Long)

@SerialVersionUID(1L)
case class FileData(id: Long, accessSalt: String, uploadedBlocksCount: Int, length: Long)
