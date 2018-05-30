package com.secretapp.backend.models

@SerialVersionUID(1L)
case class FileLocation(fileId: Long, accessHash: Long)

@SerialVersionUID(1L)
case class FileData(id: Long, accessSalt: String, length: Long, adapterData: Array[Byte])

@SerialVersionUID(1L)
case class FileBlock(fileId: Long, offset: Long, length: Long, adapterData: Array[Byte])
