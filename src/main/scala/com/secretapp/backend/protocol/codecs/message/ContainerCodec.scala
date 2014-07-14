package com.secretapp.backend.protocol.codecs.message

import scala.annotation.tailrec
import scala.collection.immutable.Seq
import com.secretapp.backend.data.transport.MessageBox
import com.secretapp.backend.data.message._
import scodec.bits.BitVector
import scodec.Codec
import scalaz._
import Scalaz._

object ContainerCodec extends Codec[Container] {
  private val nestedError = "container can't be nested"

  def encode(c : Container) = {
    val encodeMessages : Seq[String \/ BitVector] = c.messages.map {
      case MessageBox(_, Container(_)) => nestedError.left
      case m : MessageBox => MessageBoxCodec.encode(m)
    }
    foldEither(encodeMessages)(BitVector.empty)(_ ++ _)
  }

  def decode(buf : BitVector) = {
    @tailrec @inline
    def messages(buf : BitVector)(acc : Seq[MessageBox]) : String \/ Seq[MessageBox] = {
      MessageBoxCodec.decode(buf) match {
        case \/-((remain, m)) => m match {
          case MessageBox(_, Container(_)) => nestedError.left
          case m : MessageBox =>
            val res = acc :+ m
            if (remain.isEmpty) {
              res.right
            } else {
              messages(remain)(res)
            }
        }
        case l@(-\/(_)) => l
      }
    }
    messages(buf)(Seq()) match {
      case \/-(msgs) => (BitVector.empty, Container(msgs)).right
      case l@(-\/(_)) => l
    }
  }

  @tailrec @inline
  private def foldEither[A, B](seq : Seq[A \/ B])(acc : B)(f : (B, B) => B) : A \/ B = seq match {
    case x :: xs => x match {
      case \/-(r) => foldEither(xs)(f(acc, r))(f)
      case l@(-\/(_)) => l
    }
    case Nil => acc.right
  }
}
