package utils

import models.JsonListResponse
import play.api.libs.json._
import org.joda.time.DateTime

object JsonImplicits {
  implicit val longWrites = new Writes[Long] {
    def writes(n: Long) = JsString(n.toString)
  }

  implicit val dateTimeWrites = new Writes[DateTime] {
    def writes(t: DateTime) = JsString("")
  }

  implicit val jsonListResponseWrites = Json.writes[JsonListResponse]

  def toJson[A](items: Seq[A], totalCount: Int)(implicit writes: Writes[A]) =
    Json.toJson(JsonListResponse(items.map(Json.toJson(_)), totalCount))
}
