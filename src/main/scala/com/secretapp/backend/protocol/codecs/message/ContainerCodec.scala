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
  def encode(c : Container) = {
    val encodeMessages : Seq[String \/ BitVector] = c.messages.map {
      case MessageBox(_, Container(_)) => "container can't be nested".left
      case m : MessageBox => MessageBoxCodec.encode(m)
    }
    foldEithers(encodeMessages)(BitVector.empty)(_ ++ _)
  }

  def decode(buf : BitVector) = ???

  @tailrec
  private def foldEithers[A, B](items : Seq[A \/ B])(acc : B)(f : (B, B) => B) : A \/ B = items match {
    case x :: xs => x match {
      case \/-(r) => foldEithers(xs)(f(acc, r))(f)
      case l@(-\/(_)) => l
    }
    case Nil => acc.right
  }
}
