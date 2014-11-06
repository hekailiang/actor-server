package com.secretapp.backend.util

import scala.annotation.tailrec
import scalaz._
import Scalaz._

object Helpers {
  @tailrec @inline
  def foldEither[A, B](list: List[A \/ B])(acc: B)(f: (B, B) => B): A \/ B = list match {
    case x :: xs => x match {
      case \/-(r) => foldEither(xs)(f(acc, r))(f)
      case l@(-\/(_)) => l
    }
    case Nil => acc.right
  }
}
