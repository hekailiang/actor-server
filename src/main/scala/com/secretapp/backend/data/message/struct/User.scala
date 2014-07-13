package com.secretapp.backend.data.message.struct

import scala.collection.immutable.Seq
import com.secretapp.backend.data.message.ProtobufMessage
import com.secretapp.backend.data.types.Sex

case class User(id : Int,
                accessHash : Long,
                firstName : String,
                lastName : Option[String],
                sex : Option[Sex],
                keyHashes : Seq[Long]) extends ProtobufMessage
