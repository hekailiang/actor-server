package com.secretapp.backend.data.message.update

import com.secretapp.backend.data.message.struct.Config

@SerialVersionUID(1L)
case class UpdateConfig(config: Config) extends SeqUpdateMessage {
  val header = UpdateConfig.header

  def userIds = Set()
}

object UpdateConfig extends SeqUpdateMessageObject {
  val header = 0x2A
}
