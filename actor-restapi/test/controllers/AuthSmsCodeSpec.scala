package controllers

import json.CommonJsonFormats._
import json._
import org.specs2.mutable._
import play.api.libs.json._
import play.api.test.Helpers._
import play.api.test._
import utils.{CassandraSpecification, SpecUtils, Gen}
import com.secretapp.backend.models
import com.secretapp.backend.persist

class AuthSmsCodeSpec extends Specification with CassandraSpecification with SpecUtils {

  "DELETE /authsmscodes/:phone" in {

    def request(phone: Long) = FakeRequest(DELETE, s"/authsmscodes/$phone")

    "on invalid input" should {

      "return no content if code does not exists" in new WithApplication {
        implicit val req = request(Gen.genPhone)
        responseStatus must_== NO_CONTENT
      }

      "throw BadRequestException if phone is invalid" in new WithApplication {
        implicit val invalidRequest = request(42)
        responseStatus must throwA[errors.BadRequestException]
      }

    }

    "on valid input" should {

      "return no content" in new WithApplication {
        implicit val req = request(createAuthSmsCode(Gen.genAuthSmsCode).phoneNumber)
        responseStatus must_== NO_CONTENT
      }

      "remove code" in new WithApplication {
        val code = Gen.genAuthSmsCode
        val phone = createAuthSmsCode(code).phoneNumber
        implicit val req = request(phone)
        performRequest()
        persist.AuthSmsCode.getEntity(phone).sync must beNone
      }
    }

  }

  "PUT /authsmscodes/:phone" in {

    def request(phone: Long) = FakeRequest(PUT, s"/authsmscodes/$phone")

    "on invalid input" should {

      "throw JsResultException on invalid input" in new WithApplication {
        implicit val req = request(Gen.genPhone).withBody(Json.obj("bad" -> 42))
        performRequest() must throwA[JsResultException]
      }

    }

    "on valid input" should {

      "return created if code was created" in new WithApplication {
        val code = Gen.genAuthSmsCode
        val j = Json.toJson(code).as[JsObject] - "phoneNumber"
        implicit val req = request(code.phoneNumber).withBody(j)
        responseStatus must_== CREATED
      }

      "return created code if code was created" in new WithApplication {
        val code = Gen.genAuthSmsCode
        val reqJson = Json.toJson(code).as[JsObject] - "phoneNumber"
        implicit val req = request(code.phoneNumber).withBody(reqJson)
        responseJson must_== Json.toJson(code)
      }

      "create code if it does not already exists" in new WithApplication {
        val code = Gen.genAuthSmsCode
        val j = Json.toJson(code).as[JsObject] - "phoneNumber"
        implicit val req = request(code.phoneNumber).withBody(j)
        performRequest()
        persist.AuthSmsCode.getEntity(code.phoneNumber).sync.defined must_== code
      }

      "return ok if code was updated" in new WithApplication {
        val existingCode = createAuthSmsCode(Gen.genAuthSmsCode)
        val newCode = existingCode.copy(smsCode="new code")
        val j = Json.toJson(newCode).as[JsObject] - "phoneNumber"
        implicit val req = request(newCode.phoneNumber).withBody(j)
        responseStatus must_== OK
      }

      "return updated code if code was updated" in new WithApplication {
        val existingCode = createAuthSmsCode(Gen.genAuthSmsCode)
        val newCode = existingCode.copy(smsCode="new code")
        val reqJson = Json.toJson(newCode).as[JsObject] - "phoneNumber"
        implicit val req = request(newCode.phoneNumber).withBody(reqJson)
        responseJson must_== Json.toJson(newCode)
      }

      "update code if it already exists" in new WithApplication {
        val existingCode = createAuthSmsCode(Gen.genAuthSmsCode)
        val newCode = existingCode.copy(smsCode="new code")
        val j = Json.toJson(newCode).as[JsObject] - "phoneNumber"
        implicit val req = request(newCode.phoneNumber).withBody(j)
        performRequest()
        persist.AuthSmsCode.getEntity(newCode.phoneNumber).sync.defined must_== newCode
      }

    }
  }

  "GET /authsmscodes/:phone" in {

    def request(phone: Long) = FakeRequest(GET, s"/authsmscodes/$phone")

    "on invalid input" should {

      "throw NotFoundException if code does not exists" in new WithApplication {
        implicit val invalidRequest = request(Gen.genPhone)
        performRequest() must throwA[errors.NotFoundException]
      }

    }

    "on valid input" should {

      "return ok" in new WithApplication {
        val c = createAuthSmsCode(Gen.genAuthSmsCode)
        implicit val validRequest = request(c.phoneNumber)
        responseStatus must_== OK
      }

      "return code" in new WithApplication {
        val c = createAuthSmsCode(Gen.genAuthSmsCode)
        implicit val validRequest = request(c.phoneNumber)
        responseJson must_== Json.obj(
          "phoneNumber" -> c.phoneNumber,
          "smsCode"     -> c.smsCode,
          "smsHash"     -> c.smsHash
        )
      }
    }
  }

  "GET /authsmscodes" in {

    "return count codes" in new WithApplication {
      Stream.continually(createAuthSmsCode(Gen.genAuthSmsCode)).take(6).toSet
      implicit val req = FakeRequest(GET, "/authsmscodes?count=2")
      responseJson.as[JsArray].value must haveLength(2)
    }

  }

}
