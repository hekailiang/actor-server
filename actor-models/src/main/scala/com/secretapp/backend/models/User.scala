package com.secretapp.backend.models

import com.google.protobuf.ByteString
import im.actor.server.protobuf.{ ProtobufMessage, ProtobufMessageObject }
import im.actor.server.protobuf.{ Models => PBModels }
import net.sandrogrzicic.scalabuff.Message
import scala.collection.immutable
import scala.collection.JavaConverters._
import scala.language.postfixOps
import scodec.bits.BitVector

sealed trait UserState {
  def toInt: Int
}

object UserState {
  @SerialVersionUID(1L)
  case object Registered extends UserState {
    def toInt = 1
  }

  @SerialVersionUID(1L)
  case object Email extends UserState {
    def toInt = 2
  }

  @SerialVersionUID(1L)
  case object Deleted extends UserState {
    def toInt = 3
  }

  def fromInt(i: Int): UserState = i match {
    case 1 => Registered
    case 2 => Email
    case 3 => Deleted
  }
}

object User extends ProtobufMessageObject[PBModels.User, User] {
  val parseMessageFrom = PBModels.User.parseFrom: Array[Byte] => PBModels.User

  def fromMessage(m: PBModels.User): User = {
    User(
      uid = m.getUserId,
      authId = m.getAuthId,
      publicKeyHash = m.getPublicKeyHash,
      publicKeyData = BitVector(m.getPublicKey.toByteArray()),
      phoneNumber = m.getPhoneNumber,
      accessSalt = m.getAccessSalt,
      name = m.getName,
      countryCode = m.getCountryCode,
      sex = Sex.fromInt(m.getSex.getNumber),
      phoneIds = m.getPhoneIdsList().asScala.toSet map (Integer2int),
      emailIds = m.getEmailIdsList().asScala.toSet.toSet map (Integer2int),
      state = UserState.fromInt(m.getState.getNumber),
      publicKeyHashes = m.getKeyHashesList().asScala.toSet map (Long2long)
    )
  }
}

@SerialVersionUID(1L)
case class User(
  uid: Int,
  authId: Long,
  publicKeyHash: Long,
  publicKeyData: BitVector,
  phoneNumber: Long,
  accessSalt: String,
  name: String,
  countryCode: String,
  sex: Sex,
  phoneIds: immutable.Set[Int],
  emailIds: immutable.Set[Int],
  state: UserState,
  publicKeyHashes: immutable.Set[Long]
) extends ProtobufMessage[PBModels.User] {
  override lazy val asMessage =
    PBModels.User.newBuilder()
      .setUserId(uid)
      .setAuthId(authId)
      .setPublicKeyHash(publicKeyHash)
      .setPublicKey(ByteString.copyFrom(publicKeyData.toByteArray))
      .setPhoneNumber(phoneNumber)
      .setAccessSalt(accessSalt)
      .setName(name)
      .setCountryCode(countryCode)
      .setSex(PBModels.Sex.valueOf(sex.toInt))
      .addAllPhoneIds(phoneIds map (int2Integer) asJava)
      .addAllEmailIds(emailIds map (int2Integer) asJava)
      .setState(PBModels.UserState.valueOf(state.toInt))
      .addAllKeyHashes(publicKeyHashes map (long2Long) asJava)
      .build()
}
