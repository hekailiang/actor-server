package com.secretapp.backend.data.types

import scala.language.implicitConversions
import com.getsecretapp.{ proto => protobuf }

sealed trait Sex {
  def toProto: protobuf.Sex.EnumVal
}
case object NoSex extends Sex {
  def toProto = protobuf.Sex.UNKNOWN
}
case object Male extends Sex {
  def toProto = protobuf.Sex.MALE
}
case object Female extends Sex {
  def toProto = protobuf.Sex.FEMALE
}

object Sex {
  def fromProto(pb: protobuf.Sex.EnumVal): Sex = pb match {
    case protobuf.Sex.MALE => Male
    case protobuf.Sex.FEMALE => Female
    case _ => NoSex
  }
}

trait SexImplicits {
  implicit def intToSex(i: Int) = i match {
    case 1 => NoSex
    case 2 => Male
    case 3 => Female
  }

  implicit def sexToInt(s: Sex) = s match {
    case NoSex => 1
    case Male => 2
    case Female => 3
  }
}
