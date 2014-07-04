package com.secretapp.backend.data

import scala.language.implicitConversions

sealed trait Sex
case object NoSex extends Sex
case object Man extends Sex
case object Woman extends Sex

trait SexImplicits {
  implicit def IntToSex(i: Int) = i match {
    case 0 => NoSex
    case 1 => Man
    case 2 => Woman
  }

  implicit def SexToInt(s: Sex) = s match {
    case NoSex => 0
    case Man => 1
    case Woman => 2
  }
}
