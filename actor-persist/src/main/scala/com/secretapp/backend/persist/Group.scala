package com.secretapp.backend.persist

import com.secretapp.backend.models
import org.joda.time.DateTime
import scala.concurrent._
import scalikejdbc._

case class TitleChangeMeta(userId: Int, date: DateTime, randomId: Long)
case class AvatarChangeMeta(userId: Int, date: DateTime, randomId: Long)

object Group extends SQLSyntaxSupport[models.Group] {
  override val tableName = "groups"
  override val columnNames = Seq(
    "id",
    "creator_user_id",
    "access_hash",
    "title",
    "created_at",
    "title_changer_user_id",
    "title_changed_at",
    "title_change_random_id",
    "avatar_changer_user_id",
    "avatar_changed_at",
    "avatar_change_random_id"
  )

  lazy val g = Group.syntax("g")

  def apply(g: SyntaxProvider[models.Group])(rs: WrappedResultSet): models.Group = apply(g.resultName)(rs)

  def apply(g: ResultName[models.Group])(rs: WrappedResultSet): models.Group = models.Group(
    id = rs.int(g.id),
    creatorUserId = rs.int(g.creatorUserId),
    accessHash = rs.long(g.accessHash),
    title = rs.string(g.title),
    createdAt = rs.get[DateTime](g.createdAt)
  )

  type GroupWithAvatarAndChangeMeta = (models.Group, models.AvatarData, TitleChangeMeta, AvatarChangeMeta)

  def applyAvatarChangeMeta(g: ResultName[models.Group])(rs: WrappedResultSet): AvatarChangeMeta = AvatarChangeMeta(
    userId = rs.int(g.column("avatar_changer_user_id")),
    date = rs.get[DateTime](g.column("avatar_changed_at")),
    randomId = rs.long(g.column("avatar_change_random_id"))
  )

  def applyTitleChangeMeta(g: ResultName[models.Group])(rs: WrappedResultSet): TitleChangeMeta = TitleChangeMeta(
    userId = rs.int(g.column("title_changer_user_id")),
    date = rs.get[DateTime](g.column("title_changed_at")),
    randomId = rs.long(g.column("title_change_random_id"))
  )

  def applyWithAvatarAndChangeMeta(ad: models.AvatarData)(g: SyntaxProvider[models.Group])(rs: WrappedResultSet): GroupWithAvatarAndChangeMeta =
    applyWithAvatarAndChangeMeta1(ad)(g.resultName)(rs)

  def applyWithAvatarAndChangeMeta1(ad: models.AvatarData)(g: ResultName[models.Group])(rs: WrappedResultSet):
      GroupWithAvatarAndChangeMeta =
    (
      apply(g)(rs),
      ad,
      applyTitleChangeMeta(g)(rs),
      applyAvatarChangeMeta(g)(rs)
    )

  def findSync(id: Int)(
    implicit session: DBSession
  ): Option[models.Group] =
    withSQL {
      select.from(Group as g)
        .where.eq(g.id, id)
    }.map(Group(g)).single.apply

  def find(id: Int)(
    implicit ec: ExecutionContext, session: DBSession = Group.autoSession
  ): Future[Option[models.Group]] = Future {
    blocking {
      findSync(id)
    }
  }

  // TODO: run group and avatar queries in parallel
  def findWithAvatar(id: Int)(
    implicit ec: ExecutionContext, session: DBSession = Group.autoSession
  ): Future[Option[(models.Group, models.AvatarData)]] = Future {
    blocking {
      findSync(id) map { group =>
        val adOpt = AvatarData.findSync[models.Group](id)
        val ad = adOpt.getOrElse(models.AvatarData.empty)
        (group, ad)
      }
    }
  }

  // TODO: run group and avatar queries in parallel
  def findWithAvatarAndChangeMeta(id: Int)(
    implicit ec: ExecutionContext, session: DBSession = Group.autoSession
  ): Future[Option[GroupWithAvatarAndChangeMeta]] = Future {
    blocking {
      val ad = AvatarData.findSync[models.Group](id).getOrElse(models.AvatarData.empty)

      withSQL {
        select.from(Group as g)
          .where.eq(g.id, id)
      }.map(Group.applyWithAvatarAndChangeMeta(ad)(g)).single.apply
    }
  }

  def create(
    id: Int,
    creatorUserId: Int,
    accessHash: Long,
    title: String,
    createdAt: DateTime,
    randomId: Long
  )(
    implicit ec: ExecutionContext, session: DBSession = Group.autoSession
  ): Future[models.Group] = Future {
    blocking {
      withSQL {
        insert.into(Group).namedValues(
          column.id -> id,
          column.creatorUserId -> creatorUserId,
          column.accessHash -> accessHash,
          column.title -> title,
          column.createdAt -> createdAt,
          column.column("title_changer_user_id") -> creatorUserId,
          column.column("title_changed_at") -> createdAt,
          column.column("title_change_random_id") -> randomId,
          column.column("avatar_changer_user_id") -> creatorUserId,
          column.column("avatar_changed_at") -> createdAt,
          column.column("avatar_change_random_id") -> randomId
        )
      }.execute.apply

      models.Group(id, creatorUserId, accessHash, title, createdAt)
    }
  }

  def updateTitle(id: Int, title: String, userId: Int, randomId: Long, date: DateTime)(
    implicit ec: ExecutionContext, session: DBSession = Group.autoSession
  ): Future[Int] = Future {
    blocking {
      withSQL {
        update(Group).set(
          column.title -> title,
          column.column("title_changer_user_id") -> userId,
          column.column("title_change_random_id") -> randomId,
          column.column("title_changed_at") -> date
        ).where.eq(column.id, id)
      }.update.apply
    }
  }

  def updateAvatarSync(id: Int, a: models.Avatar, userId: Int, randomId: Long, date: DateTime)(
    implicit session: DBSession
  ): Int = {
    val ad = a.avatarData

    AvatarData.saveSync[models.Group](id, ad)

    withSQL {
      update(Group).set(
        column.column("avatar_changer_user_id") -> userId,
        column.column("avatar_change_random_id") -> randomId,
        column.column("avatar_changed_at") -> date
      ).where.eq(column.id, id)
    }.update.apply()
  }

  def updateAvatar(id: Int, a: models.Avatar, userId: Int, randomId: Long, date: DateTime)(
    implicit ec: ExecutionContext, session: DBSession = Group.autoSession
  ): Future[Int] = Future {
    blocking {
      updateAvatarSync(id, a, userId, randomId, date)(session)
    }
  }

  def removeAvatar(id: Int, userId: Int, randomId: Long, date: DateTime)(
    implicit ec: ExecutionContext, session: DBSession = Group.autoSession
  ): Future[Int] = updateAvatar(id, models.Avatar.empty, userId, randomId, date)
}
