package com.secretapp.backend.persist

import com.secretapp.backend.models
import com.websudos.phantom.Implicits._
import scala.concurrent.Future
import scala.collection.immutable
import scodec.bits.BitVector
import scalaz._
import Scalaz._

sealed class User extends CassandraTable[User, models.User] {
  override val tableName = "users"

  object userId extends IntColumn(this) with PartitionKey[Int] {
    override lazy val name = "user_id"
  }
  object authId extends LongColumn(this) with PrimaryKey[Long] {
    override lazy val name = "auth_id"
  }
  object publicKeyHash extends LongColumn(this) {
    override lazy val name = "public_key_hash"
  }
  object publicKey extends BlobColumn(this) {
    override lazy val name = "public_key"
  }
  object keyHashes extends SetColumn[User, models.User, Long](this) with StaticColumn[Set[Long]] {
    override lazy val name = "key_hashes"
  }
  object accessSalt extends StringColumn(this) with StaticColumn[String] {
    override lazy val name = "access_salt"
  }
  object phoneNumber extends LongColumn(this) with StaticColumn[Long] {
    override lazy val name = "phone_number"
  }
  object name extends StringColumn(this) with StaticColumn[String] {
    override lazy val name = "name"
  }
  object sex extends IntColumn(this) with StaticColumn[Int]
  object countryCode extends StringColumn(this) with StaticColumn[String] {
    override lazy val name = "country_code"
  }
  object smallAvatarFileId extends OptionalIntColumn(this) with StaticColumn[Option[Int]] {
    override lazy val name = "small_avatar_file_id"
  }
  object smallAvatarFileHash extends OptionalLongColumn(this) with StaticColumn[Option[Long]] {
    override lazy val name = "small_avatar_file_hash"
  }
  object smallAvatarFileSize extends OptionalIntColumn(this) with StaticColumn[Option[Int]] {
    override lazy val name = "small_avatar_file_size"
  }
  object largeAvatarFileId extends OptionalIntColumn(this) with StaticColumn[Option[Int]] {
    override lazy val name = "large_avatar_file_id"
  }
  object largeAvatarFileHash extends OptionalLongColumn(this) with StaticColumn[Option[Long]] {
    override lazy val name = "large_avatar_file_hash"
  }
  object largeAvatarFileSize extends OptionalIntColumn(this) with StaticColumn[Option[Int]] {
    override lazy val name = "large_avatar_file_size"
  }
  object fullAvatarFileId extends OptionalIntColumn(this) with StaticColumn[Option[Int]] {
    override lazy val name = "full_avatar_file_id"
  }
  object fullAvatarFileHash extends OptionalLongColumn(this) with StaticColumn[Option[Long]] {
    override lazy val name = "full_avatar_file_hash"
  }
  object fullAvatarFileSize extends OptionalIntColumn(this) with StaticColumn[Option[Int]] {
    override lazy val name = "full_avatar_file_size"
  }
  object fullAvatarWidth extends OptionalIntColumn(this) with StaticColumn[Option[Int]] {
    override lazy val name = "full_avatar_width"
  }
  object fullAvatarHeight extends OptionalIntColumn(this) with StaticColumn[Option[Int]] {
    override lazy val name = "full_avatar_height"
  }

  override def fromRow(row: Row): models.User =
    models.User(
      uid                 = userId(row),
      authId              = authId(row),
      publicKeyHash       = publicKeyHash(row),
      publicKey           = BitVector(publicKey(row)),
      keyHashes           = keyHashes(row),
      accessSalt          = accessSalt(row),
      phoneNumber         = phoneNumber(row),
      name                = name(row),
      sex                 = models.Sex.fromInt(sex(row)),
      countryCode         = countryCode(row),
      smallAvatarFileId   = smallAvatarFileId(row),
      smallAvatarFileHash = smallAvatarFileHash(row),
      smallAvatarFileSize = smallAvatarFileSize(row),
      largeAvatarFileId   = largeAvatarFileId(row),
      largeAvatarFileHash = largeAvatarFileHash(row),
      largeAvatarFileSize = largeAvatarFileSize(row),
      fullAvatarFileId    = fullAvatarFileId(row),
      fullAvatarFileHash  = fullAvatarFileHash(row),
      fullAvatarFileSize  = fullAvatarFileSize(row),
      fullAvatarWidth     = fullAvatarWidth(row),
      fullAvatarHeight    = fullAvatarHeight(row)
    )
}

object User extends User with TableOps {

  def insertEntityWithChildren(entity: models.User)(implicit session: Session): Future[models.User] = {
    val phone = models.Phone(
      number = entity.phoneNumber,
      userId = entity.uid,
      userAccessSalt = entity.accessSalt,
      userName = entity.name,
      userSex = entity.sex)

    val userPK = models.UserPublicKey(
      userId = entity.uid,
      publicKeyHash = entity.publicKeyHash,
      userAccessSalt = entity.accessSalt,
      publicKey = entity.publicKey,
      authId = entity.authId)

    insert
      .value(_.userId, entity.uid)
      .value(_.authId, entity.authId)
      .value(_.publicKeyHash, entity.publicKeyHash)
      .value(_.publicKey, entity.publicKey.toByteBuffer)
      .value(_.keyHashes, immutable.Set(entity.publicKeyHash))
      .value(_.accessSalt, entity.accessSalt)
      .value(_.phoneNumber, entity.phoneNumber)
      .value(_.name, entity.name)
      .value(_.sex, entity.sex.toInt)
      .value(_.countryCode, entity.countryCode)
      .value(_.smallAvatarFileId, entity.smallAvatarFileId)
      .value(_.smallAvatarFileHash, entity.smallAvatarFileHash)
      .value(_.smallAvatarFileSize, entity.smallAvatarFileSize)
      .value(_.largeAvatarFileId, entity.largeAvatarFileId)
      .value(_.largeAvatarFileHash, entity.largeAvatarFileHash)
      .value(_.largeAvatarFileSize, entity.largeAvatarFileSize)
      .value(_.fullAvatarFileId, entity.fullAvatarFileId)
      .value(_.fullAvatarFileHash, entity.fullAvatarFileHash)
      .value(_.fullAvatarFileSize, entity.fullAvatarFileSize)
      .value(_.fullAvatarWidth, entity.fullAvatarWidth)
      .value(_.fullAvatarHeight, entity.fullAvatarHeight)
      .future()
      .flatMap(_ => Phone.insertEntity(phone))
      .flatMap(_ => UserPublicKey.insertEntity(userPK))
      .flatMap(_ => AuthId.insertEntity(models.AuthId(entity.authId, Some(entity.uid))))
      .map( _ => entity)
  }

  def insertEntityRowWithChildren(userId: Int, authId: Long, publicKey: BitVector, publicKeyHash: Long, phoneNumber: Long, name: String, countryCode: String, sex: models.Sex = models.NoSex)
                                 (implicit session: Session): Future[ResultSet] =
    insert.value(_.userId, userId)
      .value(_.authId, authId)
      .value(_.publicKeyHash, publicKeyHash)
      .value(_.publicKey, publicKey.toByteBuffer)
      .value(_.name, name)
      .value(_.sex, sex.toInt)
      .value(_.countryCode, countryCode)
      .future()
      .flatMap(_ => addKeyHash(userId, publicKeyHash, phoneNumber))
      .flatMap(_ => UserPublicKey.insertEntityRow(userId, publicKeyHash, publicKey, authId))
      .flatMap(_ => AuthId.insertEntity(models.AuthId(authId, userId.some)))
      .flatMap(_ => Phone.updateUserName(phoneNumber, name))

  private def addKeyHash(userId: Int, publicKeyHash: Long, phoneNumber: Long)(implicit session: Session) =
    update.where(_.userId eqs userId).modify(_.keyHashes add publicKeyHash).future()

  /**
   * Marks keyHash as deleted in [[UserPublicKey]] and, if result is success,
   * removes keyHash from the following records: [[UserPublicKey]], [[Phone]].
   *
   * @param userId user id
   * @param publicKeyHash user public key hash
   * @param optKeepAuthId authId value to protect its row in UserRecord from deletion
   * @return a Future containing Some(authId) if removal succeeded and None if keyHash was not found
   */
  def removeKeyHash(userId: Int, publicKeyHash: Long, optKeepAuthId: Option[Long])(implicit session: Session): Future[Option[Long]] = {
    UserPublicKey.setDeleted(userId, publicKeyHash) flatMap {
      case Some(authId) =>
        val frmUser = optKeepAuthId match {
          case Some(keepAuthId) if keepAuthId == authId =>
            Future.successful()
          case _ =>
            delete.where(_.userId eqs userId).and(_.authId eqs authId).future()
        }

        Future.sequence(
          Vector(
            update.where(_.userId eqs userId).modify(_.keyHashes remove publicKeyHash).future(),
            frmUser
          )
        ) map (_ => Some(authId))
      case None =>
        Future.successful(None)
    }
  }

  def updateAvatar(userId: Int, avatar: models.Avatar)(implicit session: Session) =
    update.where(_.userId eqs userId)
      .modify(_.smallAvatarFileId   setTo avatar.smallImage.map(_.fileLocation.fileId.toInt))
      .and   (_.smallAvatarFileHash setTo avatar.smallImage.map(_.fileLocation.accessHash))
      .and   (_.smallAvatarFileSize setTo avatar.smallImage.map(_.fileSize))
      .and   (_.largeAvatarFileId   setTo avatar.largeImage.map(_.fileLocation.fileId.toInt))
      .and   (_.largeAvatarFileHash setTo avatar.largeImage.map(_.fileLocation.accessHash))
      .and   (_.largeAvatarFileSize setTo avatar.largeImage.map(_.fileSize))
      .and   (_.fullAvatarFileId    setTo avatar.fullImage.map(_.fileLocation.fileId.toInt))
      .and   (_.fullAvatarFileHash  setTo avatar.fullImage.map(_.fileLocation.accessHash))
      .and   (_.fullAvatarFileSize  setTo avatar.fullImage.map(_.fileSize))
      .and   (_.fullAvatarWidth     setTo avatar.fullImage.map(_.width))
      .and   (_.fullAvatarHeight    setTo avatar.fullImage.map(_.height))
      .future

  def updateName(userId: Int, name: String)(implicit session: Session) =
    update.where(_.userId eqs userId)
      .modify(_.name setTo name)
      .future

  def getEntity(userId: Int)(implicit session: Session): Future[Option[models.User]] =
    select.where(_.userId eqs userId).one()

  def getEntity(userId: Int, authId: Long)(implicit session: Session): Future[Option[models.User]] =
    select.where(_.userId eqs userId).and(_.authId eqs authId).one()

  def getAccessSaltAndPhone(userId: Int)(implicit session: Session): Future[Option[(String, Long)]] =
    select(_.accessSalt, _.phoneNumber).where(_.userId eqs userId).one()

  def byUid(userId: Int)(implicit session: Session): Future[Seq[models.User]] =
    select.where(_.userId eqs userId).fetch()

  def dropEntity(uid: Int)(implicit session: Session): Future[Unit] =
    delete.where(_.userId eqs uid).future() map (_ => ())

  def list(startUidExclusive: Int, count: Int)(implicit session: Session): Future[Seq[models.User]] =
    select.where(_.userId gtToken startUidExclusive).limit(count).fetch()

  def list(count: Int)(implicit session: Session): Future[Seq[models.User]] =
    select.one flatMap {
      case Some(first) => select.where(_.userId gteToken first.uid).limit(count).fetch()
      case _           => Future.successful(Seq())
    }

  def list(startUidExclusive: Option[Int], count: Int)(implicit session: Session): Future[Seq[models.User]] =
    startUidExclusive.fold(list(count))(list(_, count))
}
