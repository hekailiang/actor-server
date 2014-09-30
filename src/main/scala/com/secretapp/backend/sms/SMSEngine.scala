package com.secretapp.backend.sms

import scala.concurrent.Future

trait SMSEngine {
  def send(phoneNumber: Long, text: String): Future[String]
}
