package com.secretapp.backend.api

import scala.concurrent.Future

trait GooglePush {

  def sendGooglePush(): Future[Unit] = Future.successful()

}
