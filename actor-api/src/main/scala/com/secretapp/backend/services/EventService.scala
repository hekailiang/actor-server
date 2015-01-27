package com.secretapp.backend.services

import com.secretapp.backend.models.log.Event._
import com.secretapp.backend.persist.events.LogEvent
import scala.concurrent.ExecutionContext

object EventService {
  def log(authId: Long, phoneNumber: Long, message: EventMessage)(implicit ec: ExecutionContext) =
    LogEvent.create(
      authId = authId,
      phoneNumber = phoneNumber,
      email = "",
      klass = message.klass,
      jsonBody = message.toJson
    )
}
