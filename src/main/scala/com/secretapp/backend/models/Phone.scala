package com.secretapp.backend.models

import scala.collection.immutable
import com.secretapp.backend.data.types._

@SerialVersionUID(1L)
case class Phone(number: Long, userId: Int, userAccessSalt: String, userName: String,
                 userSex: Sex = NoSex)
