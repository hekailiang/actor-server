package com.secretapp.backend.services

import com.secretapp.backend.models

trait UserManagerService {
  protected var currentUser: Option[models.User] = None

  def getUser = currentUser
}
