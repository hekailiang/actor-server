package com.secretapp.backend.data

import com.secretapp.backend.persist.KeyedEntity

case class SessionId(authId : Long, sessionId : Long) extends KeyedEntity[(Long, Long)] {
  override val key = (authId, sessionId)
}
