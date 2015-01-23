import play.api._
import play.api.libs.json.{JsResultException, Json}
import play.api.mvc._
import play.api.mvc.Results._
import scala.concurrent.Future

object Global extends GlobalSettings {

//  override def onError(request: RequestHeader, ex: Throwable): Future[Result] =
//    Future.successful(
//      ex match {
//        case e: JsResultException     => BadRequest(Json.toJson(Error("Parse error")))
//        case e: NotFoundException     => NotFound
//        case BadRequestException(msg) => BadRequest(Json.toJson(Error(msg)))
//        case _                        => InternalServerError(Json.toJson(Error("Internal error")))
//      }
//    )

}
