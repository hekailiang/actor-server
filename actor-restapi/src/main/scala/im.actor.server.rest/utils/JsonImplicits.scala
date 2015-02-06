package im.actor.server.rest.utils

import com.secretapp.backend.persist.events.LogEventStatItem
import im.actor.server.rest.models._
import spray.json._
import org.joda.time.DateTime

object JsonImplicits extends DefaultJsonProtocol {
  implicit object LongWrites extends RootJsonFormat[Long] {
    def write(n: Long) = JsString(n.toString)
    def read(v: JsValue) = ???
  }

  implicit object DateTimeWrites extends RootJsonFormat[DateTime] {
    def write(t: DateTime) = JsString(t.toDateTimeISO.toString)
    def read(v: JsValue) = ???
  }

  implicit val jsonListResponseWrites = jsonFormat2(JsonListResponse)

  implicit val logEventStatItemWrites = jsonFormat5(LogEventStatItem)

  implicit val userItemWrites = jsonFormat8(UserItem.apply)

  def toJson[A](items: Seq[A], totalCount: Int)(implicit f: JsonWriter[A]) =
    JsonListResponse(items.map(_.toJson), totalCount).toJson.asJsObject

  case class JsonError(message: String)

  implicit val jsonErrorWrites = jsonFormat1(JsonError)

  object JsonErrors {
    val NotFound = JsonError("not found").toJson.asJsObject
  }
}
