package com.secretapp.backend.services

import com.secretapp.backend.data.models.User

trait UserManagerService {
  protected var currentUser: Option[(Long, User)] = _

  def getUser = currentUser
}
