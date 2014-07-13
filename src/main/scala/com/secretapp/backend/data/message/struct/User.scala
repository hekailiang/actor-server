package com.secretapp.backend.data.message.struct

import com.secretapp.backend.data.message.ProtobufMessage
import com.secretapp.backend.data.types.Sex

case class User(id : Int, accessHash : Long, firstName : String, lastName : String, sex : Sex, keyHashes : List[Long]) extends ProtobufMessage
