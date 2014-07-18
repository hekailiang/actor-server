package com.secretapp.backend.util

import scala.util.Random

trait RandomService {
  lazy val rand = new Random()
}
