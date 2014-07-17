package com.secretapp.backend.data.models

import com.secretapp.backend.data.types._

case class User(accessHash: Long,
                firstName: String,
                lastName: Option[String],
                sex: Option[Sex]/*,
                keyHashes: Seq[Long]*/)
