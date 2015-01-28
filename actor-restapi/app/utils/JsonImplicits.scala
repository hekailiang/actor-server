package utils

import models._
import play.api.libs.json._
import org.joda.time.DateTime

object JsonImplicits {
  implicit val longWrites = new Writes[Long] {
    def writes(n: Long) = JsString(n.toString)
  }

  implicit val dateTimeWrites = new Writes[DateTime] {
    def writes(t: DateTime) = JsString(t.toDateTimeISO.toString)
  }

  implicit val jsonListResponseWrites = Json.writes[JsonListResponse]

  implicit val seq1Writes = new Writes[Seq[(String, Int)]] {
    def writes(s: Seq[(String, Int)]) =
      JsArray(s.map { i => JsArray(Seq(JsString(i._1), JsNumber(i._2))) })
  }
  implicit val seq2Writes = new Writes[Seq[(String, Int, Int)]] {
    def writes(s: Seq[(String, Int, Int)]) =
      JsArray(s.map { i => JsArray(Seq(JsString(i._1), JsNumber(i._2), JsNumber(i._3))) })
  }

  implicit val statLogsWrites = Json.writes[StatLogs]

  implicit val userItemWrites = Json.writes[UserItem]

  def toJson[A](items: Seq[A], totalCount: Int)(implicit writes: Writes[A]) =
    Json.toJson(JsonListResponse(items.map(Json.toJson(_)), totalCount))
}
