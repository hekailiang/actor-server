package com.secretapp.backend.protocol.transport

import akka.util.ByteString
import com.secretapp.backend.data.transport.JsonPackage
import com.secretapp.backend.util.parser.BS._
import scodec.bits.BitVector
import scala.util.{Failure, Success, Try}
import scalaz._
import Scalaz._

object JsonPackageCodec {
  def encodeValid(p: JsonPackage) = {
    ByteString(s"[${p.authId},${p.sessionId},") ++ ByteString(p.messageBoxBytes.toByteBuffer) ++ ByteString("]")
  }

  def decode(data: ByteString): String \/ JsonPackage = {
    val jsonParser = for {
      _ <- skipTill(c => c == '[' || isWhiteSpace(c) || c == '"')
      authId <- parseSigned
      _ <- skipTill (c => c == ',' || isWhiteSpace(c) || c == '"')
      sessionId <- parseSigned
      _ <- skipTill (c => c == ',' || isWhiteSpace(c) || c == '"')
      _ <- skipRightTill(c => c == ']' || isWhiteSpace(c))
      message <- slice
    } yield JsonPackage(authId, sessionId, BitVector(message.toByteBuffer))
    Try(runParser(jsonParser, data)) match {
      case Success(jp) => jp.right
      case Failure(e) =>
        // TODO: silentClose ???
        s"""Expected JSON format: ["authId", "sessionId", {/* message box */}]\nDebug: ${e.getMessage}""".left
    }
  }

  def decode(data: BitVector): String \/ JsonPackage = decode(ByteString(data.toByteBuffer))
}
