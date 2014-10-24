package im.actor.export2js

import im.actor.export2js.macros.JsonType._
import scala.util.Random

object CoffeeScriptBackend {
  def apply(sealedKlasses: Seq[JsonSealedClass]): String = {
    val outBuf = new StringBuilder(s"# Automatically generated at ${new java.util.Date()}\n")
    outBuf.append(topBlock)
    val sealedClassNames = sealedKlasses.map(_.name)
    val sealedChildNames = sealedKlasses.flatMap(_.child.map(_.name))
    for (sealedKlass <- sealedKlasses) {
      sealedKlass.child.foreach { klass =>
        outBuf.append(genKlass(klass, sealedClassNames, sealedChildNames))
      }
      outBuf.append(genSealedKlass(sealedKlass))
    }
    val exportKlasses = sealedKlasses.filter(_.child.exists(_.header.isDefined)).map(_.name) ++ sealedChildNames
    outBuf.append(genExport("ActorMessages", exportKlasses))
    outBuf.mkString.replaceAll("\\s+$", "\n\n")
  }

  private def genKlass(klass: JsonClass, sealedClassNames: Seq[String], sealedChildNames: Seq[String]): String = {
    val staticHeader = klass.header.map { h => s"@header = 0x${h.toHexString}" }.getOrElse("")
    val knownKinds = Seq('Base64) ++ sealedClassNames ++ sealedChildNames
    def wrapField(name: String, kind: String, encode: Boolean, index: Int = 0): String = kind match {
      case "Base64" =>
        if (encode) s"base64.encode($name)"
        else s"base64.decode($name)"
      case s if s.startsWith("Seq[") =>
        val itemKind = kind.replaceAll("(^Seq\\[|\\]$)", "")
        if (!itemKind.endsWith("]") && !knownKinds.contains(itemKind)) name
        else {
          val itemName = s"item$index"
          s"_.map($name, ($itemName) -> { ${wrapField(itemName, itemKind, encode, index + 1)} })"
        }
      case n if !encode && sealedClassNames.contains(n) => s"$n.deserialize($name)"
      case n if sealedChildNames.contains(n) =>
        if (encode) s"$name.serialize()"
        else s"$n.deserialize($name)"
      case _ => name
    }
    val serializeBlock = klass.fields.map { f =>
      s"${f.name}: ${wrapField(s"@${f.name}", f.kind, encode = true)}"
    }.mkString(", ")
    val deserializeBlock = klass.fields.map { f => wrapField(s"body['${f.name}']", f.kind, encode = false) }.mkString(", ")

    s"""
      |class ${klass.name}
      |  $staticHeader
      |  constructor: (${klass.fields.map(f => s"@${f.name}").mkString(", ")}) ->
      |
      |  serialize: () ->
      |    { $serializeBlock }
      |
      |  @deserialize: (body) ->
      |    new ${klass.name}( $deserializeBlock )
      |
    """.stripMargin.replaceAll("\n\\s+\n", "\n")
  }

  private def genSealedKlass(klass: JsonSealedClass): String = {
    val child = klass.child.filter(_.header.isDefined)
    if (child.isEmpty) ""
    else {
      val whenBlock = child.sortBy(_.header.get).map { c => s"when ${c.header.get} then ${c.name}" }
      s"""
        |class ${klass.name}
        |  @deserialize: (body) ->
        |    header = parseInt(body['body']['header'], 10)
        |    nestedBody = body['body']['body']
        |    res = switch header
        |      ${whenBlock.mkString("\n      ")}
        |      else throw new Error("Unknown message header: #{header}")
        |    res.deserialize(nestedBody)
        |
      """.stripMargin
    }
  }

  private def genExport(namespace: String, klassNames: Seq[String]): String = {
    s"\nwindow['$namespace'] = { ${klassNames.sorted.map { n => s"$n: $n" }.mkString(", ")} }\n"
  }

  private val topBlock =
    """
      |base64 = forge.util
      |
      |toJSON = (klass, body) ->
      |  { header: klass.constructor.header, body: if body? then body else {} }
      |
    """.stripMargin
}
