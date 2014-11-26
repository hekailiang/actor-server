package com.secretapp.backend.services

import com.secretapp.backend.services.common.RandomService

trait GeneratorService extends RandomService {
  def genNewAuthId = rand.nextLong

  def genSmsCode = rand.nextLong().toString.dropWhile(c => c == '0' || c == '-').take(6)

  def genSmsHash = rand.nextLong().toString

  def genUserId = rand.nextInt(java.lang.Integer.MAX_VALUE) // TODO: akka service for ID's

  def genUserAccessSalt = rand.nextString(30)

  def genFileAccessSalt = rand.nextString(30)
}
