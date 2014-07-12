package com.secretapp.backend.data.types

import scala.language.implicitConversions

sealed trait Sex
case object NoSex extends Sex
case object Male extends Sex
case object Female extends Sex

trait SexImplicits {
  implicit def intToSex(i: Int) = i match {
    case 0 => NoSex
    case 1 => Male
    case 2 => Female
  }

  implicit def sexToInt(s: Sex) = s match {
    case NoSex => 0
    case Male => 1
    case Female => 2
  }
}
