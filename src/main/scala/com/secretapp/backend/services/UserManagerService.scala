package com.secretapp.backend.services

import com.secretapp.backend.data.models.User

trait UserManagerService {

  protected var currentUser: Option[User] = _

  def getUser = currentUser
}
