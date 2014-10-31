package com.secretapp.backend.models

import im.actor.messenger.{ api => protobuf }

sealed trait Sex {
  def toProto: protobuf.Sex.EnumVal
  def value: Int = toProto.id
  def toOption: Option[Sex]
}

@SerialVersionUID(1L)
case object NoSex extends Sex {
  def toProto = protobuf.Sex.UNKNOWN
  def toOption = None
}

@SerialVersionUID(1L)
case object Male extends Sex {
  def toProto = protobuf.Sex.MALE
  def toOption = Some(Male)
}

@SerialVersionUID(1L)
case object Female extends Sex {
  def toProto = protobuf.Sex.FEMALE
  def toOption = Some(Female)
}

object Sex {
  def fromInt(i: Int): Sex = i match {
    case protobuf.Sex.MALE.id => Male
    case protobuf.Sex.FEMALE.id => Female
    case _ => NoSex
  }

  def fromProto(e: protobuf.Sex.EnumVal): Sex = e match {
    case protobuf.Sex.MALE => Male
    case protobuf.Sex.FEMALE => Female
    case protobuf.Sex.UNKNOWN => NoSex
  }
}
