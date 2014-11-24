package com.secretapp.backend.api.rpc

import com.secretapp.backend.data.message.rpc.Error

object RpcErrors {
  val invalidAccessHash = Error(401, "ACCESS_HASH_INVALID", "Invalid access hash.", canTryAgain = false)
  val cantAddSelf = Error(401, "OWN_USER_ID", "User id cannot be equal to self.", canTryAgain = false)
  def entityNotFound(entity: String) = Error(401, s"${entity.toUpperCase}_NOT_FOUND", s"$entity not found.", canTryAgain = false)
  def entityAlreadyExists(entity: String) = Error(409, s"${entity.toUpperCase}_ALREADY_EXISTS", s"$entity already exists.", canTryAgain = false)
}
