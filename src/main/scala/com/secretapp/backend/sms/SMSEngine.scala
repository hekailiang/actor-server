package com.secretapp.backend.sms

import scala.concurrent.Future

trait SMSEngine {
  def send(number: String, text: String): Future[String]
}
