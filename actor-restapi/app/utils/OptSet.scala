package utils

import scalaz._
import Scalaz._

case class OptSet[A](value: A) {

  def optSet[B](o: Option[B])(f: (A, B) => A) =
    o some { f(value, _) } none value

}

object OptSet {

  implicit def toOptSet[A](value: A): OptSet[A] =
    OptSet(value)

  implicit def fromOptSet[A](optSet: OptSet[A]): A =
    optSet.value

}
