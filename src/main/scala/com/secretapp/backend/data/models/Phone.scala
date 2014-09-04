package com.secretapp.backend.data.models

import scala.concurrent.Future
import scala.collection.immutable
import com.datastax.driver.core.Session
import com.secretapp.backend.persist._
import com.secretapp.backend.data.types._

case class Phone(number: Long, userId: Int, userAccessSalt: String, userName: String,
                 userKeyHashes: immutable.Set[Long], userSex: Sex = NoSex)
