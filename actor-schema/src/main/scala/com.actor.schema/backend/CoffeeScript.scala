package com.actor.schema.backend

import com.actor.schema.AST._
import scala.annotation.tailrec

trait Backend {
  val ext: String

  def gen(packageName: String, abstractKlasses: Seq[AbstractKlass], klasses: Seq[Klass], enums: Seq[Enum]): StringBuilder
}

object CoffeeScript extends Backend {
  private def genKlass(klass: Klass) = {
    val parent = klass.parentKlass.map { n => s" extends $n" }.getOrElse("")
    val fields = klass.fields.map { field =>
      s"""${field.name}: { n: ${field.id}, type: "${field.kind}", rule: "${field.rule.toString.toLowerCase}" }"""
    }.mkString("\n    ")

    @tailrec
    def f(fields: List[Field], applyFields: Seq[String], applyHeaders: Seq[String]): (String, String) = fields match {
      case x :: xs => xs match {
        case y :: ys if x.isTrait =>
          f(ys, applyFields :+ x.name, applyHeaders :+ s"@${y.name} = ${x.name}.constructor.header")
        case yss => f(yss, applyFields :+ x.name, applyHeaders)
      }
      case Nil => (applyFields.reverse.map { f => s"@$f" }.mkString(", "), applyHeaders.reverse.mkString("\n    "))
    }
    val (applyFields, applyBody) = f(klass.fields.reverse.toList, Seq(), Seq())

    s"""
      |class ${klass.name}$parent
      |  ${klass.header.map { h => s"@header: 0x${h.toHexString}" }.getOrElse("")}
      |
      |  @schema:
      |    ${if (fields.isEmpty) "{}" else fields }
      |
      |  constructor: ($applyFields) ->
      |    $applyBody
      |
      |  encode: () ->
      |    Protobuf.encode(@)
      |
      |  @decode: (bodyBytes) ->
      |    Protobuf.decode("${klass.name}", bodyBytes)
      |
    """.stripMargin.replaceAll("\\n+\\s+\\n+", "\n\n")
  }

  private def genKlasses(klasses: Seq[Klass]) = {
    val buf = new StringBuilder
    klasses foreach { klass => buf.append(genKlass(klass)) }
    buf
  }

  private def genAbstractKlass(klass: AbstractKlass) = {
    val codec = klass.headerSize match {
      case HeaderSize.Byte => "int8"
      case HeaderSize.Int => "int32"
    }

    s"""
      |class ${klass.name}
      |  @encode: (msg, buf) ->
      |    buf = ByteBuffer.alloc(buf)
      |    $codec.encode(msg.constructor.header, buf)
      |    bytes.encode(msg.encode(), buf)
      |    buf.flipChain()
      |    buf
      |
      |  @decode: (buf) ->
      |    header = $codec.decode(buf).value
      |    bodyBytes = bytes.decode(buf).value
      |    child = [${klass.child.map(_.name).mkString(",")}]
      |    for klass in child
      |      return klass.decode(bodyBytes) if header == klass.header
      |    throw new Error("Unknown message header: #{header}")
      |
    """.stripMargin
  }

  private def genAbstractKlasses(klasses: Seq[AbstractKlass]) = {
    val buf = new StringBuilder
    klasses foreach { klass => buf.append(genAbstractKlass(klass)) }
    buf
  }

  private def genEnums(enums: Seq[Enum]): StringBuilder = {
    val buf = new StringBuilder
    enums foreach { enum =>
      buf.append(
        s"""
          |class ${enum.name}
          |  ${enum.values.map { case ((key, value)) => s"@$key: $value" }.mkString("\n  ")}
          |
          |  @schema: [${enum.values.map { case ((key, _)) => s"'$key'" }.mkString(",")}]
          |
        """.stripMargin)
    }
    buf
  }

  val ext = "coffee"

  def gen(packageName: String, abstractKlasses: Seq[AbstractKlass], klasses: Seq[Klass], enums: Seq[Enum]) = {
    val buf = new StringBuilder
    buf.append(s"# Automatically generated at ${new java.util.Date()}\n\n")
    buf.append(genAbstractKlasses(abstractKlasses))
    buf.append(genEnums(enums))
    buf.append(genKlasses(klasses))
    buf.append(s"\nwindow.Protobuf$packageName = @\n")
    buf
  }
}
