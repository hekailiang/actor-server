package com.secretapp.backend.api.rpc

import com.secretapp.backend.data.message.rpc.Error

object RpcErrors {
  val invalidAccessHash = Error(401, "ACCESS_HASH_INVALID", "Invalid access hash.", canTryAgain = false)
  def entityNotFound(entity: String) = Error(401, s"${entity.toUpperCase}_NOT_FOUND", s"$entity not found.", canTryAgain = false)
}