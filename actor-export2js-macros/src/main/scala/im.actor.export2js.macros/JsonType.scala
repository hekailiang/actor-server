package im.actor.export2js.macros

package object JsonType {
  case class JsonField(name: String, kind: String)

  case class JsonSealedClass(name: String, child: Seq[JsonClass])

  case class JsonClass(name: String, header: Option[Int], fields: Seq[JsonField])
}
