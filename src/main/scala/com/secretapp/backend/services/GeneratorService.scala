package com.secretapp.backend.services

import com.secretapp.backend.services.common.RandomService

trait GeneratorService extends RandomService {
  def genNewAuthId = rand.nextLong

  def genSmsCode = rand.nextLong().toString.drop(1).take(6)

  def genSmsHash = rand.nextLong().toString

  def genUserId = rand.nextInt
}
