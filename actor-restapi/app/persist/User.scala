package persist

import com.datastax.driver.core.Row
import com.websudos.phantom.Implicits._
import play.api.Logger
import scodec.bits.BitVector

import scala.concurrent.Future
import scala.util.Random
import scalaz._
import Scalaz._
import errors.NotFoundException.getOrNotFound
import com.secretapp.backend.{models => m}

sealed class User extends CassandraTable[persist.User, models.User] {

  override lazy val tableName = "users"

  object id extends IntColumn(this) with PartitionKey[Int] {
    override lazy val name = "uid"
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
    override lazy val name = "first_name"
  }

  object sex extends IntColumn(this) with StaticColumn[Int]

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

  private val optFileLocation = Applicative[Option].lift2(m.FileLocation.apply)

  private val optAvatarImage = Applicative[Option].lift4(m.AvatarImage.apply)

  private def optAvatar(
    smallImage: Option[m.AvatarImage],
    largeImage: Option[m.AvatarImage],
    fullImage:  Option[m.AvatarImage]
  ): Option[models.Avatar] =
    (smallImage |@| largeImage |@| fullImage) { (_, _, _) =>
      models.Avatar(smallImage, largeImage, fullImage)
    }

  override def fromRow(row: Row): models.User =
    models.User(
      id(row),
      authId(row),
      publicKeyHash(row),
      BitVector(publicKey(row)),
      phoneNumber(row),
      accessSalt(row),
      name(row),
      models.Sex.fromInt(sex(row)),
      optAvatar(
        optAvatarImage(
          optFileLocation(smallAvatarFileId(row).map(_.toLong), smallAvatarFileHash(row)),
          100.some,
          100.some,
          smallAvatarFileSize(row)
        ),
        optAvatarImage(
          optFileLocation(largeAvatarFileId(row).map(_.toLong), largeAvatarFileHash(row)),
          200.some,
          200.some,
          largeAvatarFileSize(row)
        ),
        optAvatarImage(
          optFileLocation(fullAvatarFileId(row).map(_.toLong), fullAvatarFileHash(row)),
          fullAvatarWidth(row),
          fullAvatarHeight(row),
          fullAvatarFileSize(row)
        )
      ),
      keyHashes(row)
    )

}

object User extends User with DbConnector {

  private def saveQuery(u: models.User) =
    insert
      .value(_.id,                  u.id)
      .value(_.authId,              u.authId)
      .value(_.publicKeyHash,       u.publicKeyHash)
      .value(_.publicKey,           u.publicKey.toByteBuffer)
      .value(_.phoneNumber,         u.phoneNumber)
      .value(_.accessSalt,          u.accessSalt)
      .value(_.name,                u.name)
      .value(_.sex,                 u.sex.toInt)
      .value(_.smallAvatarFileId,   u.avatar.flatMap(_.smallImage.map(_.fileLocation.fileId.toInt)))
      .value(_.smallAvatarFileHash, u.avatar.flatMap(_.smallImage.map(_.fileLocation.accessHash)))
      .value(_.smallAvatarFileSize, u.avatar.flatMap(_.smallImage.map(_.fileSize)))
      .value(_.largeAvatarFileId,   u.avatar.flatMap(_.largeImage.map(_.fileLocation.fileId.toInt)))
      .value(_.largeAvatarFileHash, u.avatar.flatMap(_.largeImage.map(_.fileLocation.accessHash)))
      .value(_.largeAvatarFileSize, u.avatar.flatMap(_.largeImage.map(_.fileSize)))
      .value(_.fullAvatarFileId,    u.avatar.flatMap(_.fullImage.map(_.fileLocation.fileId.toInt)))
      .value(_.fullAvatarFileHash,  u.avatar.flatMap(_.fullImage.map(_.fileLocation.accessHash)))
      .value(_.fullAvatarFileSize,  u.avatar.flatMap(_.fullImage.map(_.fileSize)))
      .value(_.fullAvatarWidth,     u.avatar.flatMap(_.fullImage.map(_.width)))
      .value(_.fullAvatarHeight,    u.avatar.flatMap(_.fullImage.map(_.height)))
      .value(_.keyHashes,           u.keyHashes)

  def save(u: models.User): Future[models.User] =
    saveQuery(u).future() map (_ => u)

  private def saveIfNotExists(u: models.User): Future[Option[models.User]] =
    saveQuery(u).ifNotExists.future() map (_.wasApplied option u)

  private def trySaveUntilSucceed(us: Seq[models.User]): Future[Option[models.User]] =
    us.headOption some { u =>
      Logger.trace(s"Trying to persist user with uid ${u.id}")
      saveIfNotExists(u) flatMap {
        _.fold(trySaveUntilSucceed(us.tail))(Future successful _.some)
      }
    } none Future.successful(None)

  def create(u: models.User): Future[Option[models.User]] = {
    val tries = 10
    val usersWithRandomId = Stream.fill(tries)(u.copy(id = Random.nextInt()))

    trySaveUntilSucceed(usersWithRandomId) map {
      case None =>
        Logger.warn(s"Failed to persist user with unique uid after $tries tries")
        None
      case pu   =>
        Logger.trace(s"User persisted successfully")
        pu
    }
  }

  def byId(id: Int): Future[Option[models.User]] =
    select.where(_.id eqs id).one()

  def getById(id: Int): Future[models.User] =
    getOrNotFound(byId(id))

  def remove(id: Int): Future[Unit] =
    delete.where(_.id eqs id).future() map (_ => ())

  def list(startIdExclusive: Int, count: Int): Future[Seq[models.User]] =
    select.where(_.id gtToken startIdExclusive).limit(count).fetch()

  def list(count: Int): Future[Seq[models.User]] =
    select.one flatMap {
      case Some(first) => select.where(_.id gteToken first.id).limit(count).fetch()
      case _           => Future.successful(Seq())
    }

  def list(startIdExclusive: Option[Int], count: Int): Future[Seq[models.User]] =
    startIdExclusive.fold(list(count))(list(_, count))

}
