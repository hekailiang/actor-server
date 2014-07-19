package com.secretapp.backend.services.common

import akka.event.LoggingAdapter

trait LoggerService {
  def log: LoggingAdapter
}
