package com.secretapp.backend.persist

import com.secretapp.backend.models
import com.datastax.driver.core.querybuilder.QueryBuilder
import com.websudos.phantom.Implicits._
import com.websudos.phantom.query.SelectQuery
import scala.concurrent.Future
import scodec.bits.BitVector

case class TitleChangeMeta(userId: Int, date: Long, randomId: Long)
case class AvatarChangeMeta(userId: Int, date: Long, randomId: Long)

sealed class Group extends CassandraTable[Group, models.Group] {
  override val tableName = "groups"

  object id extends IntColumn(this) with PartitionKey[Int]
  object creatorUserId extends IntColumn(this) {
    override lazy val name = "creator_user_id"
  }
  object accessHash extends LongColumn(this) {
    override lazy val name = "access_hash"
  }
  object title extends StringColumn(this)
  object createDate extends LongColumn(this) {
    override lazy val name = "create_date"
  }

  object titleChangeUserId extends IntColumn(this) {
    override lazy val name = "title_change_user_id"
  }

  object titleChangeDate extends LongColumn(this) {
    override lazy val name = "title_change_date"
  }

  object titleChangeRandomId extends LongColumn(this) {
    override lazy val name = "title_change_random_id"
  }

  object avatarChangeUserId extends IntColumn(this) {
    override lazy val name = "avatar_change_user_id"
  }

  object avatarChangeDate extends LongColumn(this) {
    override lazy val name = "avatar_change_date"
  }

  object avatarChangeRandomId extends LongColumn(this) {
    override lazy val name = "avatar_change_random_id"
  }

  object smallAvatarFileId extends OptionalIntColumn(this) {
    override lazy val name = "small_avatar_file_id"
  }
  object smallAvatarFileHash extends OptionalLongColumn(this) {
    override lazy val name = "small_avatar_file_hash"
  }
  object smallAvatarFileSize extends OptionalIntColumn(this) {
    override lazy val name = "small_avatar_file_size"
  }
  object largeAvatarFileId extends OptionalIntColumn(this) {
    override lazy val name = "large_avatar_file_id"
  }
  object largeAvatarFileHash extends OptionalLongColumn(this) {
    override lazy val name = "large_avatar_file_hash"
  }
  object largeAvatarFileSize extends OptionalIntColumn(this) {
    override lazy val name = "large_avatar_file_size"
  }
  object fullAvatarFileId extends OptionalIntColumn(this) {
    override lazy val name = "full_avatar_file_id"
  }
  object fullAvatarFileHash extends OptionalLongColumn(this) {
    override lazy val name = "full_avatar_file_hash"
  }
  object fullAvatarFileSize extends OptionalIntColumn(this) {
    override lazy val name = "full_avatar_file_size"
  }
  object fullAvatarWidth extends OptionalIntColumn(this) {
    override lazy val name = "full_avatar_width"
  }
  object fullAvatarHeight extends OptionalIntColumn(this) {
    override lazy val name = "full_avatar_height"
  }

  override def fromRow(row: Row): models.Group = {
    models.Group(
      id            = id(row),
      creatorUserId = creatorUserId(row),
      accessHash    = accessHash(row),
      title         = title(row),
      createDate    = createDate(row)
    )
  }

  def fromRowWithAvatar(row: Row): (models.Group, models.AvatarData) = {
    (
      fromRow(row),
      models.AvatarData(
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
    )
  }

  def fromRowWithAvatarAndChangeMeta(row: Row): (models.Group, models.AvatarData, TitleChangeMeta, AvatarChangeMeta) = {
    (
      fromRow(row),
      models.AvatarData(
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
      ),
      TitleChangeMeta(userId = titleChangeUserId(row), date = titleChangeDate(row), randomId = titleChangeRandomId(row)),
      AvatarChangeMeta(userId = avatarChangeUserId(row), date = avatarChangeDate(row), randomId = avatarChangeRandomId(row))
    )
  }

  def selectWithAvatar: SelectQuery[Group, (models.Group, models.AvatarData)] =
    new SelectQuery[Group, (models.Group, models.AvatarData)](
      this.asInstanceOf[Group],
      QueryBuilder.select().from(tableName),
      this.asInstanceOf[Group].fromRowWithAvatar
    )

  def selectWithAvatarAndChangeMeta: SelectQuery[Group, (models.Group, models.AvatarData, TitleChangeMeta, AvatarChangeMeta)] =
    new SelectQuery[
      Group,
      (models.Group, models.AvatarData, TitleChangeMeta, AvatarChangeMeta)
    ](
      this.asInstanceOf[Group],
      QueryBuilder.select().from(tableName), this.asInstanceOf[Group].fromRowWithAvatarAndChangeMeta
    )
}

object Group extends Group with TableOps {
  def insertEntity(entity: models.Group, randomId: Long)(implicit session: Session): Future[ResultSet] =
    insert
      .value(_.id, entity.id)
      .value(_.creatorUserId, entity.creatorUserId)
      .value(_.accessHash, entity.accessHash)
      .value(_.title, entity.title)
      .value(_.createDate, entity.createDate)
      .value(_.titleChangeUserId, entity.creatorUserId)
      .value(_.titleChangeDate, entity.createDate)
      .value(_.titleChangeRandomId, randomId)
      .value(_.avatarChangeUserId, entity.creatorUserId)
      .value(_.avatarChangeDate, entity.createDate)
      .value(_.avatarChangeRandomId, randomId)
      .future()

  def dropEntity(groupId: Int)(implicit session: Session): Future[Unit] =
    delete.where(_.id eqs groupId).future() map (_ => ())

  def getEntity(groupId: Int)(implicit session: Session): Future[Option[models.Group]] =
    select.where(_.id eqs groupId).one()

  def getEntityWithAvatar(groupId: Int)
    (implicit session: Session): Future[Option[(models.Group, models.AvatarData)]] =
    selectWithAvatar.where(_.id eqs groupId).one()

  def getEntityWithAvatarAndChangeMeta(groupId: Int)
    (implicit session: Session): Future[Option[(models.Group, models.AvatarData, TitleChangeMeta, AvatarChangeMeta)]] = {
    selectWithAvatarAndChangeMeta.where(_.id eqs groupId).one()
  }

  def updateTitle(id: Int, title: String, userId: Int, randomId: Long, date: Long)(implicit session: Session): Future[ResultSet] =
    update
      .where(_.id eqs id)
      .modify(_.title setTo title)
      .and(_.titleChangeUserId setTo userId)
      .and(_.titleChangeRandomId setTo randomId)
      .and(_.titleChangeDate setTo date)
      .future()

  def updateAvatar(id: Int, avatar: models.Avatar, userId: Int, randomId: Long, date: Long)(implicit session: Session) =
    update.where(_.id eqs id)
      .modify(_.avatarChangeUserId setTo userId)
      .and   (_.avatarChangeRandomId setTo randomId)
      .and   (_.avatarChangeDate setTo date)
      .and   (_.smallAvatarFileId   setTo avatar.smallImage.map(_.fileLocation.fileId.toInt))
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

  def removeAvatar(id: Int, userId: Int, randomId: Long, date: Long)(implicit session: Session) =
    updateAvatar(id, models.Avatar(None, None, None), userId, randomId, date)
}
