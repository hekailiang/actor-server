package com.secretapp.backend.protocol.codecs.message

import scala.annotation.tailrec
import scala.collection.immutable
import com.secretapp.backend.protocol.codecs._
import com.secretapp.backend.data.transport.MessageBox
import com.secretapp.backend.data.message._
import com.secretapp.backend.util.Helpers._
import scodec.bits.BitVector
import scodec.Codec
import scalaz._
import Scalaz._

object ContainerCodec extends Codec[Container] {
  private val nestedError = "container can't be nested"

  def encode(c: Container) = {
    val encodeMessages: List[String \/ BitVector] = c.messages.map {
      case MessageBox(_, Container(_)) => nestedError.left
      case m: MessageBox => MessageBoxCodec.encode(m)
    }.toList
    for {
      res <- foldEither(encodeMessages)(BitVector.empty)(_ ++ _)
      size <- varint.encode(encodeMessages.length)
    } yield (size ++ res)
  }

  def decode(buf: BitVector) = {
    @tailrec @inline
    def messages(buf: BitVector, size: Int)(acc: Seq[MessageBox], countOfMsg: Int = 0): String \/ Seq[MessageBox] = {
      MessageBoxCodec.decode(buf) match {
        case \/-((remain, m)) => m match {
          case MessageBox(_, Container(_)) => nestedError.left
          case m: MessageBox =>
            val res = acc :+ m
            if (size == countOfMsg + 1) {
              res.right
            } else if (remain.isEmpty) {
              s"Container.count($size) doesn't match with count of messages($countOfMsg)}".left
            } else {
              messages(remain, size)(res, countOfMsg + 1)
            }
        }
        case l@(-\/(_)) => l
      }
    }

    for {
      s <- varint.decode(buf); (xs, size) = s
      msgs <- messages(xs, size.toInt)(immutable.Seq())
    } yield (BitVector.empty, Container(immutable.Seq(msgs: _*)))
  }
}
