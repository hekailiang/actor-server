package models

import models.CommonJsonFormats._
import play.api.libs.json.Json
import scodec.bits.BitVector
import scala.collection.immutable

case class User(
  id:            Int,
  authId:        Long,
  publicKeyHash: Long,
  publicKey:     BitVector,
  phoneNumber:   Long,
  accessSalt:    String,
  name:          String,
  sex:           Sex,
  avatar:        Option[Avatar]      = None,
  keyHashes:     immutable.Set[Long] = immutable.Set()
)

object User {

  implicit val jsonWrites = models.json.User

}
