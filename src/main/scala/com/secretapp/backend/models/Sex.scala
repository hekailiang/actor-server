package com.secretapp.backend.models

import scala.language.implicitConversions
import im.actor.messenger.{ api => protobuf }
import scalaz._
import Scalaz._

sealed trait Sex {
  def toProto: protobuf.Sex.EnumVal
  def toOption: Option[Sex]
  def toInt: Int
}

@SerialVersionUID(1L)
case object NoSex extends Sex {
  def toProto = protobuf.Sex.UNKNOWN
  def toOption = None
  val toInt = 1
}

@SerialVersionUID(1L)
case object Male extends Sex {
  def toProto = protobuf.Sex.MALE
  def toOption = Male.some
  val toInt = 2
}

@SerialVersionUID(1L)
case object Female extends Sex {
  def toProto = protobuf.Sex.FEMALE
  def toOption = Female.some
  val toInt = 3
}

object Sex {
  def fromProto(pb: protobuf.Sex.EnumVal): Sex = pb match {
    case protobuf.Sex.MALE => Male
    case protobuf.Sex.FEMALE => Female
    case _ => NoSex
  }

  def fromInt(i: Int): Sex = i match {
    case 1 => NoSex
    case 2 => Male
    case 3 => Female
  }
}
