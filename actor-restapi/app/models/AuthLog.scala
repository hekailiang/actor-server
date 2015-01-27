package models

import com.secretapp.backend.persist._
import com.secretapp.backend.persist.events.LogEvent
import org.joda.time.DateTime
import scala.concurrent.{ExecutionContext, Future}
import play.api.libs.json._

case class AuthLog(id: Long, authId: Long, phoneNumber: Long, email: String,
                       userId: Int, userName: String, deviceHash: String, deviceTitle: String,
                       klass: Int, jsonBody: String, createdAt: DateTime)

object AuthLog {
  implicit val authLogItemWrites = new Writes[AuthLog] {
    def writes(e: AuthLog) = JsObject(Seq(
      ("id", JsNumber(e.id)),
      ("authId", JsString(e.authId.toString)),
      ("phoneNumber", JsString(e.phoneNumber.toString)),
      ("email", JsString(e.email)),
      ("userId", JsNumber(e.userId)),
      ("userName", JsString(e.userName)),
      ("deviceHash", JsString(e.deviceHash)),
      ("deviceTitle", JsString(e.deviceTitle)),
      ("klass", JsNumber(e.klass)),
      ("body", Json.parse(e.jsonBody)),
      ("createdAt", JsString(e.createdAt.toDateTimeISO.toString))
    ))
  }

  def paginate(req: Map[String, Seq[String]] = Map())
              (implicit ec: ExecutionContext): Future[(Seq[AuthLog], Int)] =
  {
    for {
      (logEvents, totalCount) <- LogEvent.all(req)
      authSessions <- AuthSession.getDevisesData(logEvents.map(_.authId).toSet)
      userNames <- User.getNames(authSessions.map(_.userId).toSet)
    } yield {
      val authSessionsMap = authSessions.map { s => (s.authId, s) }.toMap
      val userNamesMap = userNames.toMap
      val authLogs = logEvents.map { e =>
        val s = authSessionsMap.get(e.authId)
        val userId = s.map(_.userId).getOrElse(0)
        AuthLog(
          id = e.id,
          authId = e.authId,
          phoneNumber = e.phoneNumber,
          email = e.email,
          userId = userId,
          userName = userNamesMap.getOrElse(userId, ""),
          deviceHash = s.map(_.deviceHash).getOrElse(""),
          deviceTitle = s.map(_.deviceTitle).getOrElse(""),
          klass = e.klass,
          jsonBody = e.jsonBody,
          createdAt = e.createdAt
        )
      }
      (authLogs, totalCount)
    }
  }
}
