package controllers

import models.CommonJsonFormats._
import org.specs2.mutable._
import org.specs2.specification.BeforeExample
import play.api.libs.json._
import play.api.test.Helpers._
import play.api.test._
import scodec.bits.BitVector
import utils.{CassandraSpecification, SpecUtils, Gen}
import models.json._
import com.secretapp.backend.models.{Avatar, Sex, Male, Female, NoSex}

class UserSpec extends Specification with CassandraSpecification with SpecUtils {

  val user = Gen.genUser

  "POST /users" in {

    val request = FakeRequest(POST, "/users")

    "on invalid input" should {

      implicit val invalidRequest = request.withBody(Json.obj("bad" -> "bad"))

      "throw JsResultException on invalid input" in new WithApplication {
        responseStatus must throwA[JsResultException]
      }

    }

    "on valid input" should {

      val userCreationRequest = Gen.genUserCreationRequest
      implicit val validRequest = request.withBody(Json.toJson(userCreationRequest))

      "send created" in new WithApplication {
        responseStatus must_== CREATED
      }

      "send created user back" in new WithApplication {
        responseJson.as[models.UserCreationRequest] must_== userCreationRequest
      }

      "persist user" in new WithApplication {
        val receivedUser = responseJson
        val id = (receivedUser \ "id").asOpt[Int].defined
        val persistedUser = persist.User.byId(id).sync.defined

        persistedUser.uid           must_== id
        persistedUser.authId        must_== (receivedUser \ "authId"       ).asOpt[Long].defined
        persistedUser.publicKeyHash must_== (receivedUser \ "publicKeyHash").asOpt[Long].defined
        persistedUser.publicKey     must_== (receivedUser \ "publicKey"    ).asOpt[BitVector].defined
        persistedUser.phoneNumber   must_== (receivedUser \ "phoneNumber"  ).asOpt[Long].defined
     // persistedUser.accessSalt is generated, client has no way to know it
        persistedUser.name          must_== (receivedUser \ "name"         ).asOpt[String].defined
        persistedUser.sex           must_== (receivedUser \ "sex"          ).asOpt[Sex].defined
        persistedUser.avatar        must_== (receivedUser \ "avatar"       ).asOpt[Option[Avatar]].defined
        persistedUser.keyHashes     must_== (receivedUser \ "keyHashes"    ).asOpt[Set[Long]].defined
      }

    }

  }

  "DELETE /users/:id" in {

    def request(id: Int) = FakeRequest(DELETE, s"/users/$id")

    "on invalid input" should {

      implicit val invalidRequest = request(42)

      "return no content if user does not exists" in new WithApplication {
        responseStatus must_== NO_CONTENT
      }

    }

    "on valid input" should {

      "return no content" in new WithApplication {
        implicit val req = request(createUser(user).uid)
        responseStatus must_== NO_CONTENT
      }

      "remove user" in new WithApplication {
        val id = createUser(user).uid
        implicit val req = request(id)
        performRequest()
        persist.User.byId(id).sync must beNone
      }

    }

  }

  "PUT /users/:id" in {

    def request(id: Int) = FakeRequest(PUT, s"/users/$id")

    "on invalid input" should {

      "throw NotFoundException if user does not exists" in new WithApplication {
        implicit val invalidRequest = request(42).withBody(Json.obj())
        responseStatus must throwA[errors.NotFoundException]
      }

      "throw JsResultException on invalid input" in new WithApplication {
        val id = createUser(user).uid
        implicit val invalidRequest = request(id).withBody(Json.obj("name" -> 42))
        responseStatus must throwA[JsResultException]
      }

    }

    "on valid input" should {

      "return ok on valid request" in new WithApplication {
        val u = createUser(user)
        implicit val validRequest = request(u.uid).withBody(Json.obj())
        responseStatus must_== OK
      }

      "return user intact on empty request" in new WithApplication {
        val u = createUser(user)
        implicit val validRequest = request(u.uid).withBody(Json.obj())
        responseJson must_== Json.obj(
          "id"            -> u.uid,
          "authId"        -> u.authId,
          "publicKeyHash" -> u.publicKeyHash,
          "publicKey"     -> u.publicKey,
          "phoneNumber"   -> u.phoneNumber,
          "name"          -> u.name,
          "sex"           -> u.sex,
          "countryCode"   -> u.countryCode,
          "avatar"        -> u.avatar,
          "keyHashes"     -> u.keyHashes
        )
      }

      "keep user intact on empty request" in new WithApplication {
        val u = createUser(user)
        implicit val validRequest = request(u.uid).withBody(Json.obj())
        performRequest()
        persist.User.byId(u.uid).sync.defined must_== u
      }

      "return user with name changed on name change request" in new WithApplication {
        val u = createUser(user)
        implicit val validRequest = request(u.uid).withBody(Json.obj(
          "name" -> "New Name"
        ))
        responseJson must_== Json.obj(
          "id"            -> u.uid,
          "authId"        -> u.authId,
          "publicKeyHash" -> u.publicKeyHash,
          "publicKey"     -> u.publicKey,
          "phoneNumber"   -> u.phoneNumber,
          "name"          -> "New Name",
          "sex"           -> u.sex,
          "countryCode"   -> u.countryCode,
          "avatar"        -> u.avatar,
          "keyHashes"     -> u.keyHashes
        )
      }

      "change user name on name change request" in new WithApplication {
        val u = createUser(user)
        implicit val validRequest = request(u.uid).withBody(Json.obj(
          "name" -> "New Name"
        ))
        performRequest()
        persist.User.byId(u.uid).sync.defined must_== u.copy(name = "New Name")
      }

      "return user with sex changed on sex change request" in new WithApplication {
        val u = createUser(Gen.genUser.copy(sex = Male))
        implicit val validRequest = request(u.uid).withBody(Json.obj(
          "sex" -> Female
        ))
        responseJson must_== Json.obj(
          "id"            -> u.uid,
          "authId"        -> u.authId,
          "publicKeyHash" -> u.publicKeyHash,
          "publicKey"     -> u.publicKey,
          "phoneNumber"   -> u.phoneNumber,
          "name"          -> u.name,
          "sex"           -> Female,
          "countryCode"   -> u.countryCode,
          "avatar"        -> u.avatar,
          "keyHashes"     -> u.keyHashes
        )
      }

      "change user sex on sex change request" in new WithApplication {
        val u = createUser(Gen.genUser.copy(sex = Male))
        implicit val validRequest = request(u.uid).withBody(Json.obj(
          "sex" -> Female
        ))
        performRequest()
        persist.User.byId(u.uid).sync.defined must_== u.copy(sex = Female)
      }

      // TODO: Test other changes as well.

    }

  }

  "GET /users/:id" in {

    def request(id: Int) = FakeRequest(GET, s"/users/$id")

    "on invalid input" should {

      "throw NotFoundException if user does not exists" in new WithApplication {
        implicit val invalidRequest = request(42)
        performRequest() must throwA[errors.NotFoundException]
      }

    }

    "on valid input" should {

      "return ok" in new WithApplication {
        val u = createUser(user)
        implicit val validRequest = request(u.uid)
        responseStatus must_== OK
      }

      "return user" in new WithApplication {
        val u = createUser(user)
        implicit val validRequest = request(u.uid)
        responseJson must_== Json.obj(
          "id"            -> u.uid,
          "authId"        -> u.authId,
          "publicKeyHash" -> u.publicKeyHash,
          "publicKey"     -> u.publicKey,
          "phoneNumber"   -> u.phoneNumber,
          "name"          -> u.name,
          "sex"           -> u.sex,
          "countryCode"   -> u.countryCode,
          "avatar"        -> u.avatar,
          "keyHashes"     -> u.keyHashes
        )
      }

    }

  }

}

class UserListSpec extends Specification with CassandraSpecification with SpecUtils with BeforeExample {

  override def before = {
    cleanDb()
    createDb()
  }

  "GET /users" in {

    implicit val request = FakeRequest(GET, "/users")

    "return ok" in new WithApplication {
      responseStatus must_== OK
    }

    "return count users" in new WithApplication {
      Stream.continually(createUser(Gen.genUser)).take(6).toSet
      implicit val request = FakeRequest(GET, "/users?count=2")
      responseJson.as[JsArray].value must haveLength(2)
    }

    "return all users" in new WithApplication {
      val users = Stream.continually(createUser(Gen.genUser)).take(5).toSet
      responseJson.as[JsArray].value.toSet must_== users.map(Json toJson _)
    }

    "paginate properly" in new WithApplication {
      val users = Stream.continually(createUser(Gen.genUser)).take(6).toSet
      val receivedUsers1 = {
        implicit val request = FakeRequest(GET, "/users?count=2")
        responseJson.as[JsArray].value
      }
      val receivedUsers2 = {
        val lastId = receivedUsers1.last \ "id"
        implicit val request = FakeRequest(GET, s"/users?start_id=$lastId&count=2")
        responseJson.as[JsArray].value
      }
      val receivedUsers3 = {
        val lastId = receivedUsers2.last \ "id"
        implicit val request = FakeRequest(GET, s"/users?start_id=$lastId&count=2")
        responseJson.as[JsArray].value
      }

      (receivedUsers1 ++ receivedUsers2 ++ receivedUsers3).toSet must_== users.map(Json toJson _)
    }

  }

}
