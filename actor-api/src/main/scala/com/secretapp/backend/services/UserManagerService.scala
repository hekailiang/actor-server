package com.secretapp.backend.services

import com.secretapp.backend.models.User

trait UserManagerService {
  protected var currentUser: Option[User] = None

  def getUser = currentUser
}
