import play.api._
import play.api.mvc._
import scala.concurrent.ExecutionContext.Implicits.global

object Global extends GlobalSettings {

  override def doFilter(next: EssentialAction): EssentialAction = EssentialAction { req =>
    next.apply(req).map(_.withHeaders(
      "Access-Control-Allow-Origin" -> req.headers.get("Origin").getOrElse("*"),
      "Access-Control-Allow-Methods" -> req.headers.get("Access-Control-Request-Method").getOrElse("*"),
      "Access-Control-Allow-Headers" -> req.headers.get("Access-Control-Request-Headers").getOrElse("*"),
      "Access-Control-Allow-Credentials" -> "true"
    ))
  }

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
