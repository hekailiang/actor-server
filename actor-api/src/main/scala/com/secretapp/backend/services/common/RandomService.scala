package com.secretapp.backend.services.common

import scala.util.Random

trait RandomService {
  lazy val rand = new Random()
}
