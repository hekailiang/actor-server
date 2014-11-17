import org.specs2.mutable.Specification
import play.api.libs.json.{Json, JsResultException}
import play.api.test._
import play.api.test.Helpers._
import utils.SpecUtils
import utils.CassandraSpecification

class GlobalSpec extends Specification with CassandraSpecification with SpecUtils {

  "Global error handler" in {

    def result(implicit e: Exception) = Global.onError(FakeRequest(), e)

    "on JsResultException" should {

      implicit val e: Exception = new JsResultException(Seq())

      "return bad request" in {
        status(result) must_== BAD_REQUEST
      }

      "return appropriate message" in {
        contentAsJson(result) must_== Json.obj("message" -> "Parse error")
      }

    }

    "on NotFoundException" should {

      implicit val e: Exception = new errors.NotFoundException

      "return not found" in {
        status(result) must_== NOT_FOUND
      }

    }

    "on BadRequestException" should {

      implicit val e: Exception = new errors.BadRequestException("message")

      "return bad request" in {
        status(result) must_== BAD_REQUEST
      }

      "return appropriate message" in {
        contentAsJson(result) must_== Json.obj("message" -> "message")
      }

    }

    "on any other Exception" should {

      implicit val e: Exception = new Exception()

      "return internal server error" in {
        status(result) must_== INTERNAL_SERVER_ERROR
      }

      "return appropriate message" in {
        contentAsJson(result) must_== Json.obj("message" -> "Internal error")
      }

    }

  }
}
