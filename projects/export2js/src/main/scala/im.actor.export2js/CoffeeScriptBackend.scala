package im.actor.export2js

import im.actor.export2js.macros.JsonType._

object CoffeeScriptBackend {
  def apply(sealedKlasses: Seq[JsonSealedClass]): String = {
    val outBuf = new StringBuilder(s"# Automatically generated at ${new java.util.Date()}\n")
    outBuf.append(serializableKlass)
    for (sealedKlass <- sealedKlasses) {
      sealedKlass.child.foreach { klass =>
        outBuf.append(genKlass(klass))
      }
      outBuf.append(genSealedKlass(sealedKlass))
    }
    outBuf.append(genExport("ActorMessages", sealedKlasses))
    outBuf.mkString.replaceAll("\\s+$", "\n\n")
  }

  private def wrapFields(fields: Seq[JsonField]): String = {
    s"@_field_wrappers = {}"
  }

  private def genKlass(klass: JsonClass): String = {
    val staticHeader = klass.header.map { h => s"@header = 0x${h.toHexString}" }.getOrElse("")
    s"""
      |class ${klass.name} extends Serializable
      |  $staticHeader
      |  @_fields = [${klass.fields.map(f => s"'${f.name}'").mkString(", ")}]
      |  ${wrapFields(klass.fields)}
      |  constructor: (${klass.fields.map(f => s"@${f.name}").mkString(", ")}) ->
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

  private def genExport(namespace: String, sealedKlasses: Seq[JsonSealedClass]): String = {
    val klasses = sealedKlasses.flatMap(_.child).sortBy(_.name)
    s"\nwindow['$namespace'] = { ${klasses.map { c => s"'${c.name}': ${c.name}" }.mkString(", ")} }\n"
  }

  private val serializableKlass =
    """
      |class Serializable
      |  serialize: () ->
      |    body = {}
      |    proto = @constructor
      |    for field in proto._fields
      |      body[field] = @[field]
      |    if proto.header?
      |      { header: proto.header, body: body }
      |    else
      |      body
      |
      |  @deserialize: (body) ->
      |    ins = new @prototype.constructor()
      |    for field in @_fields
      |      ins[field] = body[field]
      |    ins
      |
    """.stripMargin
}
