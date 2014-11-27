package com.secretapp.backend.sms

import scala.concurrent.Future

trait SmsEngine {
  protected def message(code: String) = s"Your actor activation code: $code"

  def send(phoneNumber: Long, code: String): Future[Unit]
}
