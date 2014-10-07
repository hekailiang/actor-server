package com.secretapp.backend.data.models

import scala.collection.immutable
import com.secretapp.backend.data.types._

@SerialVersionUID(1l)
case class Phone(number: Long, userId: Int, userAccessSalt: String, userName: String,
                 userKeyHashes: immutable.Set[Long], userSex: Sex = NoSex)
