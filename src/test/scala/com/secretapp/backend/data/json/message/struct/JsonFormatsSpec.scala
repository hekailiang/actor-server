package com.secretapp.backend.data.json.message.struct

import com.secretapp.backend.data.json.JsonSpec
import com.secretapp.backend.data.json.CommonJsonFormats._
import com.secretapp.backend.data.json.message.rpc.file.JsonFormatsSpec._
import com.secretapp.backend.models
import com.secretapp.backend.data.message.struct
import play.api.libs.json._
import scala.util.Random
import scalaz._
import Scalaz._
import JsonFormatsSpec._

class JsonFormatsSpec extends JsonSpec {

  "(de)serializer" should {

    "(de)serialize UserId" in {
      val (v, j) = genUserId
      testToAndFromJson(j, v)
    }

    "(de)serialize AvatarImage" in {
      val (v, j) = genAvatarImage
      testToAndFromJson(j, v)
    }

    "(de)serialize Avatar" in {
      val (v, j) = genAvatar
      testToAndFromJson(j, v)
    }

    "(de)serialize User" in {
      val (v, j) = genUser
      testToAndFromJson(j, v)
    }

  }

}

object JsonFormatsSpec {

  def genUserId = {
    val uid = Random.nextInt()
    val accessHash = Random.nextLong()

    (
      struct.UserId(1, 2),
      Json.obj(
        "uid"        -> 1,
        "accessHash" -> "2"
      )
    )
  }

  def genAvatarImage = {
    val (fileLocation, fileLocationJson) = genFileLocation
    val width = Random.nextInt()
    val height = Random.nextInt()
    val fileSize = Random.nextInt()

    (
      models.AvatarImage(fileLocation, width, height, fileSize),
      Json.obj(
        "fileLocation" -> fileLocationJson,
        "width"        -> width,
        "height"       -> height,
        "fileSize"     -> fileSize
      )
    )
  }

  def genAvatar = {
    val (smallImage, smallImageJson) = genAvatarImage
    val (largeImage, largeImageJson) = genAvatarImage
    val (fullImage, fullImageJson) = genAvatarImage

    (
      models.Avatar(
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

  def genUser = {
    val uid = Random.nextInt()
    val accessHash = Random.nextLong()
    val keyHash = Random.nextLong()
    val phoneNumber = Random.nextLong()
    val (avatar, avatarJson) = genAvatar

    (
      struct.User(uid, accessHash, "name", models.Male.some, Set(keyHash), phoneNumber, avatar.some),
      Json.obj(
        "uid"         -> uid,
        "accessHash"  -> accessHash.toString,
        "name"        -> "name",
        "sex"         -> "male",
        "keyHashes"   -> Json.arr(keyHash.toString),
        "phoneNumber" -> phoneNumber.toString,
        "avatar"      -> avatarJson
      )
    )
  }

}
