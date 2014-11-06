package com.secretapp.backend.protocol.codecs.message

import com.secretapp.backend.data.transport.MessageBox
import scodec.bits._
import com.secretapp.backend.data.json.message._
import play.api.libs.json.Json
import scalaz._
import Scalaz._

object JsonMessageBoxCodec {
  def encodeValid(mb: MessageBox): BitVector = {
    BitVector(Json.stringify(Json.toJson(mb)).getBytes)
  }

  def decode(buf: BitVector): String \/ MessageBox = {
    try {
      val jsonAst = Json.parse(buf.toByteArray)
      Json.fromJson[MessageBox](jsonAst).asEither match {
        case Right(mb) => mb.right
        case Left(e) => e.map { t => s"${t._1}: ${t._2.mkString}"}.mkString("\n").left
      }
    } catch { case e: Throwable => e.getMessage.left }
  }
}
