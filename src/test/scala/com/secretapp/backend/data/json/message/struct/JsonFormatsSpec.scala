package com.secretapp.backend.data.json.message.struct

import com.secretapp.backend.data.json.JsonSpec
import com.secretapp.backend.data.json.message.rpc.file.JsonFormatsSpec._
import com.secretapp.backend.data.message.struct._
import com.secretapp.backend.data.types.Male
import play.api.libs.json._
import scala.util.Random
import scalaz._
import Scalaz._
import JsonFormatsSpec._

class JsonFormatsSpec extends JsonSpec {

  "(de)serializer" should {

    "(de)serialize UserId" in {
      val v = UserId(1, 2)
      val j = Json.obj(
        "uid"        -> 1,
        "accessHash" -> "2"
      )
      testToAndFromJson[UserId](j, v)
    }

    "(de)serialize AvatarImage" in {
      val (v, j) = avatarImage
      testToAndFromJson[AvatarImage](j, v)
    }

    "(de)serialize Avatar" in {
      val (v, j) = avatar
      testToAndFromJson[Avatar](j, v)
    }

    "(de)serialize User" in {
      val (av, avJson) = avatar
      val v = User(16, 17, "name", Male.some, Set(18), 19, av.some)
      val j = Json.obj(
        "uid"         -> 16,
        "accessHash"  -> "17",
        "name"        -> "name",
        "sex"         -> "male",
        "keyHashes"   -> Json.arr("18"),
        "phoneNumber" -> "19",
        "avatar"      -> avJson
      )
      testToAndFromJson[User](j, v)
    }

  }

}

object JsonFormatsSpec {

  def avatarImage = {
    val (fl, flJson) = fileLocation
    val width = Random.nextInt()
    val height = Random.nextInt()
    val fileSize = Random.nextInt()

    (
      AvatarImage(fl, width, height, fileSize),
      Json.obj(
        "fileLocation" -> flJson,
        "width"        -> width,
        "height"       -> height,
        "fileSize"     -> fileSize
      )
    )
  }

  def avatar = {
    val (smallImage, smallImageJson) = avatarImage
    val (largeImage, largeImageJson) = avatarImage
    val (fullImage, fullImageJson) = avatarImage

    (
      Avatar(
        smallImage.some,
        largeImage.some,
        fullImage.some
      ),
      Json.obj(
        "smallImage" -> smallImageJson,
        "largeImage" -> largeImageJson,
        "fullImage" -> fullImageJson
      )
    )
  }

}
