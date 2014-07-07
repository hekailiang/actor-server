package com.secretapp.backend.data

import scala.language.implicitConversions

sealed trait Sex
case object NoSex extends Sex
case object Male extends Sex
case object Female extends Sex

trait SexImplicits {
  implicit def IntToSex(i: Int) = i match {
    case 0 => NoSex
    case 1 => Male
    case 2 => Female
  }

  implicit def SexToInt(s: Sex) = s match {
    case NoSex => 0
    case Male => 1
    case Female => 2
  }
}
