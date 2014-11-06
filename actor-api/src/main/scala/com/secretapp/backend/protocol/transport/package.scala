package com.secretapp.backend.protocol

package object transport {
  case class ParseError(msg: String) // error which caused when package parsing (we can't parse authId/sessionId/messageId)
}
