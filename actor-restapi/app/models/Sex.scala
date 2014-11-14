package models

import models.CommonJsonFormats._

sealed trait Sex {

  def toInt: Int

}

case object NoSex extends Sex {

  val toInt = 1

}

case object Male extends Sex {

  val toInt = 2

}

case object Female extends Sex {

  val toInt = 3

}

object Sex {

  implicit val jsonFormat = json.Sex

  def fromInt(i: Int) = i match {
    case 1 => NoSex
    case 2 => Male
    case 3 => Female
  }

}
