package com.secretapp.backend.persist

import com.datastax.driver.core.{ ResultSet, Row, Session }
import com.secretapp.backend.data.message.struct.Avatar
import com.websudos.phantom.Implicits._
import com.secretapp.backend.data.Implicits._
import com.secretapp.backend.data.models._
import com.secretapp.backend.crypto.ec.PublicKey
import com.secretapp.backend.data.types._
import scodec.bits.BitVector
import scala.concurrent.Future
import scalaz._
import Scalaz._

sealed class UserRecord extends CassandraTable[UserRecord, User] {
  override lazy val tableName = "users"

  object uid extends IntColumn(this) with PartitionKey[Int]
  object authId extends LongColumn(this) with PrimaryKey[Long] {
    override lazy val name = "auth_id"
  }
  object publicKeyHash extends LongColumn(this) {
    override lazy val name = "public_key_hash"
  }
  object publicKey extends BlobColumn(this) {
    override lazy val name = "public_key"
  }
  object keyHashes extends SetColumn[UserRecord, User, Long](this) with StaticColumn[Set[Long]] {
    override lazy val name = "key_hashes"
  }
  object accessSalt extends StringColumn(this) with StaticColumn[String] {
    override lazy val name = "access_salt"
  }
  object phoneNumber extends LongColumn(this) with StaticColumn[Long] {
    override lazy val name = "phone_number"
  }
  object name extends StringColumn(this) with StaticColumn[String] {
    override lazy val name = "first_name"
  }
  object sex extends IntColumn(this) with StaticColumn[Int]
  object smallAvatarFileId extends OptionalIntColumn(this) {
    override lazy val name = "small_avatar_file_id"
  }
  object smallAvatarFileHash extends OptionalLongColumn(this) {
    override lazy val name = "small_avatar_file_hash"
  }
  object largeAvatarFileId extends OptionalIntColumn(this) {
    override lazy val name = "large_avatar_file_id"
  }
  object largeAvatarFileHash extends OptionalLongColumn(this) {
    override lazy val name = "large_avatar_file_hash"
  }
  object fullAvatarFileId extends OptionalIntColumn(this) {
    override lazy val name = "full_avatar_file_id"
  }
  object fullAvatarFileHash extends OptionalLongColumn(this) {
    override lazy val name = "full_avatar_file_hash"
  }
  object fullAvatarWidth extends OptionalIntColumn(this) {
    override lazy val name = "full_avatar_width"
  }
  object fullAvatarHeight extends OptionalIntColumn(this) {
    override lazy val name = "full_avatar_height"
  }

  override def fromRow(row: Row): User = {
    User(
      uid = uid(row),
      authId = authId(row),
      publicKeyHash = publicKeyHash(row),
      publicKey = BitVector(publicKey(row)),
      keyHashes = keyHashes(row),
      accessSalt = accessSalt(row),
      phoneNumber = phoneNumber(row),
      name = name(row),
      sex = intToSex(sex(row)),
      smallAvatarFileId = smallAvatarFileId(row),
      smallAvatarFileHash = smallAvatarFileHash(row),
      largeAvatarFileId = largeAvatarFileId(row),
      largeAvatarFileHash = largeAvatarFileHash(row),
      fullAvatarFileId = fullAvatarFileId(row),
      fullAvatarFileHash = fullAvatarFileHash(row),
      fullAvatarWidth = fullAvatarWidth(row),
      fullAvatarHeight = fullAvatarHeight(row)
    )
  }
}

object UserRecord extends UserRecord with DBConnector {

  def insertEntityWithPhoneAndPK(entity: User)(implicit session: Session): Future[ResultSet] = {

    val phone = Phone(
      number = entity.phoneNumber,
      userId = entity.uid,
      userAccessSalt = entity.accessSalt,
      userKeyHashes = Set(entity.publicKeyHash),
      userName = entity.name,
      userSex = sexToInt(entity.sex))

    val userPK = UserPublicKey(
      uid = entity.uid,
      publicKeyHash = entity.publicKeyHash,
      userAccessSalt = entity.accessSalt,
      publicKey = entity.publicKey,
      authId = entity.authId)

    insert.value(_.uid, entity.uid)
      .value(_.authId, entity.authId)
      .value(_.publicKeyHash, entity.publicKeyHash)
      .value(_.publicKey, entity.publicKey.toByteBuffer)
      .value(_.keyHashes, Set(entity.publicKeyHash))
      .value(_.accessSalt, entity.accessSalt)
      .value(_.phoneNumber, entity.phoneNumber)
      .value(_.name, entity.name)
      .value(_.sex, sexToInt(entity.sex))
      .value(_.smallAvatarFileId, entity.smallAvatarFileId)
      .value(_.smallAvatarFileHash, entity.smallAvatarFileHash)
      .value(_.largeAvatarFileId, entity.largeAvatarFileId)
      .value(_.largeAvatarFileHash, entity.largeAvatarFileHash)
      .value(_.fullAvatarFileId, entity.fullAvatarFileId)
      .value(_.fullAvatarFileHash, entity.fullAvatarFileHash)
      .value(_.fullAvatarWidth, entity.fullAvatarWidth)
      .value(_.fullAvatarHeight, entity.fullAvatarHeight)
      .future().
      flatMap(_ => PhoneRecord.insertEntity(phone)).
      flatMap(_ => UserPublicKeyRecord.insertEntity(userPK)).
      flatMap(_ => AuthIdRecord.insertEntity(AuthId(entity.authId, entity.uid.some)))
  }

  def insertPartEntityWithPhoneAndPK(uid: Int, authId: Long, publicKey: BitVector, phoneNumber: Long)
                                    (implicit session: Session): Future[ResultSet] = {
    val publicKeyHash = PublicKey.keyHash(publicKey)

    insert.value(_.uid, uid)
      .value(_.authId, authId)
      .value(_.publicKeyHash, publicKeyHash)
      .value(_.publicKey, publicKey.toByteBuffer)
      .future().
      flatMap(_ => addKeyHash(uid, publicKeyHash, phoneNumber)).
      flatMap(_ => UserPublicKeyRecord.insertPartEntity(uid, publicKeyHash, publicKey, authId)).
      flatMap(_ => AuthIdRecord.insertEntity(AuthId(authId, uid.some)))
  }

  def insertPartEntityWithPhoneAndPK(uid: Int, authId: Long, publicKey: BitVector, phoneNumber: Long, name: String, sex: Sex = NoSex)
                                    (implicit session: Session): Future[ResultSet] = {
    val publicKeyHash = PublicKey.keyHash(publicKey)

    insert.value(_.uid, uid)
      .value(_.authId, authId)
      .value(_.publicKeyHash, publicKeyHash)
      .value(_.publicKey, publicKey.toByteBuffer)
      .value(_.name, name)
      .value(_.sex, sexToInt(sex))
      .future().
      flatMap(_ => addKeyHash(uid, publicKeyHash, phoneNumber)).
      flatMap(_ => UserPublicKeyRecord.insertPartEntity(uid, publicKeyHash, publicKey, authId)).
      flatMap(_ => AuthIdRecord.insertEntity(AuthId(authId, uid.some)))
  }

  private def addKeyHash(uid: Int, publicKeyHash: Long, phoneNumber: Long)(implicit session: Session) = {
    update.where(_.uid eqs uid).modify(_.keyHashes add publicKeyHash).
      future().flatMap(_ => PhoneRecord.addKeyHash(phoneNumber, publicKeyHash))
  }

  def removeKeyHash(uid: Int, publicKeyHash: Long, phoneNumber: Long)(implicit session: Session) = {
    update.where(_.uid eqs uid).modify(_.keyHashes remove publicKeyHash).
      future().flatMap(_ => PhoneRecord.removeKeyHash(phoneNumber, publicKeyHash))
  }

  def updateAvatar(authId: Long, uid: Int, avatar: Avatar)(implicit session: Session) =
    update.where(_.uid eqs uid).and(_.authId eqs authId)
      .modify(_.smallAvatarFileId   setTo avatar.smallImage.map(_.fileLocation.fileId.toInt))
      .and   (_.smallAvatarFileHash setTo avatar.smallImage.map(_.fileLocation.accessHash))
      .and   (_.largeAvatarFileId   setTo avatar.largeImage.map(_.fileLocation.fileId.toInt))
      .and   (_.largeAvatarFileHash setTo avatar.largeImage.map(_.fileLocation.accessHash))
      .and   (_.fullAvatarFileId    setTo avatar.fullImage.map(_.fileLocation.fileId.toInt))
      .and   (_.fullAvatarFileHash  setTo avatar.fullImage.map(_.fileLocation.accessHash))
      .and   (_.fullAvatarWidth     setTo avatar.fullImage.map(_.width))
      .and   (_.fullAvatarHeight    setTo avatar.fullImage.map(_.height))
      .future

  def getEntities(uid: Int)(implicit session: Session): Future[Seq[User]] = {
    select.where(_.uid eqs uid).limit(100).fetch()
  }

  def getEntity(uid: Int)(implicit session: Session): Future[Option[User]] = {
    select.where(_.uid eqs uid).one()
  }

  def getEntity(uid: Int, authId: Long)(implicit session: Session): Future[Option[User]] = {
    select.where(_.uid eqs uid).and(_.authId eqs authId).one()
  }
}
