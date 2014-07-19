package com.secretapp.backend.services

import com.secretapp.backend.services.common.RandomService

trait GeneratorService extends RandomService {
  def genNewAuthId = rand.nextLong
}
