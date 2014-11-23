package errors

import scala.concurrent.Future
import play.api.libs.concurrent.Execution.Implicits._

class NotFoundException extends Exception

object NotFoundException {

  def getOrNotFound[A](f: Future[Option[A]]): Future[A] =
    f map { _.fold(throw new errors.NotFoundException)(identity) }

}
