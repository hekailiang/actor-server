package com.secretapp.backend.persist

import com.secretapp.backend.models
import scala.concurrent.{ ExecutionContext, Future, blocking }
import scala.collection.immutable
import scalaz._; import Scalaz._
import scala.language.postfixOps
import scalikejdbc._
import scodec.bits._

object User extends SQLSyntaxSupport[models.User] with Paginator[models.User] {
  override val tableName = "users"
  override val columnNames = Seq("id", "access_salt", "name", "country_code", "sex", "state")

  lazy val u = User.syntax("u")

  def apply(
    authId: Long,
    publicKeyHash: Long,
    publicKeyData: BitVector,
    phoneNumber: Long,
    phoneIds: immutable.Set[Int],
    emailIds: immutable.Set[Int],
    publicKeyHashes: immutable.Set[Long]
  )(u: SyntaxProvider[models.User])(rs: WrappedResultSet): models.User = apply1(
    authId,
    publicKeyHash,
    publicKeyData,
    phoneNumber,
    phoneIds,
    emailIds,
    publicKeyHashes
  )(u.resultName)(rs)

  // rename to apply when we will get rid of first parameters mess
  def apply1(
    authId: Long,
    publicKeyHash: Long,
    publicKeyData: BitVector,
    phoneNumber: Long,
    phoneIds: immutable.Set[Int],
    emailIds: immutable.Set[Int],
    publicKeyHashes: immutable.Set[Long]
  )(u: ResultName[models.User])(
    rs: WrappedResultSet
  ): models.User = models.User(
    uid = rs.int(u.column("id")),
    authId = authId,
    publicKeyHash = publicKeyHash,
    publicKeyData = publicKeyData,
    phoneNumber = phoneNumber,
    accessSalt = rs.string(u.accessSalt),
    name = rs.string(u.name),
    countryCode = rs.string(u.countryCode),
    sex = models.Sex.fromInt(rs.int(u.sex)),
    phoneIds = phoneIds,
    emailIds = emailIds,
    state = models.UserState.fromInt(rs.int(u.column("state"))),
    publicKeyHashes = publicKeyHashes
  )

  def create(id: Int, accessSalt: String, name: String, countryCode: String, sex: models.Sex, state: models.UserState)(
    authId: Long,
    publicKeyHash: Long,
    publicKeyData: BitVector
  )(
    implicit
    ec: ExecutionContext, session: DBSession = User.autoSession
  ): Future[Unit] = Future {
    withSQL {
      insert.into(User).namedValues(
        column.column("id") -> id,
        column.accessSalt -> accessSalt,
        column.name -> name,
        column.countryCode -> countryCode,
        column.sex -> sex.toInt,
        column.column("state") -> state.toInt
      )
    }.execute.apply
  } flatMap { _ =>
    for {
      //_ <- Phone.insertEntity(phone)
      _ <- UserPublicKey.createOrReplace(id, publicKeyHash, publicKeyData, authId)
      _ <- AuthId.createOrUpdate(authId, Some(id))
    } yield ()
  }

  def savePartial(id: Int, name: String, countryCode: String)(
    authId: Long,
    publicKeyHash: Long,
    publicKeyData: BitVector,
    phoneNumber: Long
  )(
    implicit
      ec: ExecutionContext, session: DBSession = User.autoSession
  ): Future[Unit] = {
    withSQL {
      update(User).set(
        column.name -> name,
        column.countryCode -> countryCode
      ).where.eq(column.column("id"), id)
    }.update.apply

    for {
      _ <- AuthId.createOrUpdate(authId, Some(id))
      //_ <- Phone.updateUserName(phoneNumber, name)
      _ <- UserPublicKey.createOrReplace(id, publicKeyHash, publicKeyData, authId)
    } yield ()
  }

  def findData(id: Int)(implicit ec: ExecutionContext, session: DBSession = User.autoSession): Future[Option[models.UserData]] = {
    val mainDataFuture: Future[Option[models.BasicUserData]] = Future {
      blocking {
        withSQL {
          select.from(User as u).where.eq(u.column("id"), id)
        }.map { rs =>
          println(column)
            models.BasicUserData(id = rs.int(u.resultName.column("id")),
              accessSalt = rs.string(u.resultName.accessSalt),
              name = rs.string(u.resultName.name),
              countryCode = rs.string(u.resultName.countryCode),
              sex = models.Sex.fromInt(rs.int(u.resultName.sex)),
              state = models.UserState.fromInt(rs.int(u.resultName.column("state"))))
          }.single.apply
      }
    }

    for {
      mainDataOpt <- mainDataFuture
      keyHashes <- UserPublicKey.findAllHashesByUserId(userId = id)
      phones <- UserPhone.findAllByUserId(userId = id)
      emails <- UserEmail.findAllByUserId(userId = id)
    } yield {
      mainDataOpt map { mainData =>
        models.UserData(id = mainData.id,
          accessSalt = mainData.accessSalt,
          name = mainData.name,
          countryCode = mainData.countryCode,
          sex = mainData.sex,
          state = mainData.state,
          phoneNumber = phones.head.number,
          phoneIds = phones.map(_.id).toSet,
          emailIds = emails.map(_.id).toSet,
          publicKeyHashes = keyHashes.toSet)
      }
    }
  }

  def find(id: Int)(authId: Option[Long])(
    implicit
      ec: ExecutionContext, session: DBSession = User.autoSession
  ): Future[Option[models.User]] = {
    authId map (i => Future.successful(Some(i))) getOrElse (UserPublicKey.findFirstActiveAuthIdByUserId(userId = id)) flatMap {
      case Some(authId) =>
        val (
          pkOptFuture,
          keysFuture,
          phonesFuture,
          emailsFuture
        ) = (
          UserPublicKey.findByUserIdAndAuthId(userId = id, authId),
          UserPublicKey.findAllByUserId(userId = id),
          UserPhone.findAllByUserId(userId = id),
          UserEmail.findAllByUserId(userId = id)
        )

        val extraDataFuture = for {
          pkOpt <- pkOptFuture
          keys <- keysFuture
          phones <- phonesFuture
          emails <- emailsFuture
        } yield {
          pkOpt map { pk =>
            (pk, keys, phones, emails)
          }
        }

        extraDataFuture map {
          case Some((pk, keys, phones, emails)) =>
            withSQL {
              select.from(User as u)
                .where.eq(u.column("id"), id)
            }.map(User(
              authId = authId,
              publicKeyHash = pk.hash,
              publicKeyData = pk.data,
              phoneNumber = phones.head.number,
              phoneIds = phones map (_.id) toSet,
              emailIds = emails map (_.id) toSet,
              publicKeyHashes = keys map (_.hash) toSet
            )(u)).single.apply
          case None => None
        }
      case None => Future.successful(None)
    }
  }

  def findAllSaltsByIds(ids: immutable.Seq[Int])(
    implicit
      ec: ExecutionContext, session: DBSession = User.autoSession
  ): Future[List[(Int, String)]] = Future {
    withSQL {
      select(column.column("id"), column.accessSalt).from(User as u)
        .where.in(u.column("id"), ids)
    }.map(rs => (rs.int(column.column("id")), rs.string(column.accessSalt))).list().apply
  }

  def findWithAvatar(userId: Int)(authId: Option[Long] = None)(
    implicit
      ec: ExecutionContext, session: DBSession = User.autoSession
  ): Future[Option[(models.User, models.AvatarData)]] = {
    for {
      userOpt <- find(userId)(authId)
      adOpt <- AvatarData.find(id = userId, typ = AvatarData.typeVal[models.User])
    } yield {
      userOpt map (
        (_, adOpt getOrElse (models.AvatarData.empty))
      )
    }
  }

  def updateName(userId: Int, name: String)(
    implicit
      ec: ExecutionContext, session: DBSession = User.autoSession
  ): Future[Int] = Future {
    withSQL {
      update(User).set(
        column.name -> name
      ).where.eq(column.column("id"), userId)
    }.update.apply
  }

  def getNames(userIds: Seq[Int])
              (implicit ec: ExecutionContext, session: DBSession = User.autoSession): Future[Seq[(Int, String)]] =
  Future {
    blocking {
      select(u.column("id"), u.name).from(User as u).where.in(u.column("id"), userIds).toSQL.map { rs =>
        (rs.int("id"), rs.string("name"))
      }.list().apply()
    }
  }

  def all(req: Map[String, String])
         (implicit ec: ExecutionContext, session: DBSession = User.autoSession): Future[(Seq[(Int, String)], Int)] =
    Future {
      blocking {
        paginateWithTotal(select(u.column("id"), u.name).from(this as u).toSQLSyntax, u, req, Some(("name", ASC))) { rs =>
          (rs.int("id"), rs.string("name"))
        }
      }
    }
}
