package com.actor.schema

object AST {
  object HeaderSize extends Enumeration {
    type HeaderSize = Value
    val Int, Byte = Value
  }

  case class AbstractKlass(name: String, child: Seq[Klass], headerSize: HeaderSize.HeaderSize)

  object Rule extends Enumeration {
    type Rule = Value
    val Required, Optional, Repeated = Value
  }
  import Rule._

  case class Field(name: String, id: Int, kind: String, rule: Rule, isTrait: Boolean)

  case class Klass(name: String, parentKlass: Option[String], fields: Seq[Field], header: Option[Int])

  case class Enum(name: String, values: Map[String, Int])
}
