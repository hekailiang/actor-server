package com.secretapp.backend.persist

import com.secretapp.backend.models
import com.datastax.driver.core.querybuilder.QueryBuilder
import com.websudos.phantom.Implicits._
import com.websudos.phantom.query.SelectQuery
import scala.concurrent.Future
import scodec.bits.BitVector

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
      title         = title(row)
    )
  }

  def fromRowWithAvatar(row: Row): (models.Group, models.AvatarData) = {
    (
      models.Group(
        id            = id(row),
        creatorUserId = creatorUserId(row),
        accessHash    = accessHash(row),
        title         = title(row)
      ),
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

  def selectWithAvatar: SelectQuery[Group, (models.Group, models.AvatarData)] =
    new SelectQuery[Group, (models.Group, models.AvatarData)](this.asInstanceOf[Group], QueryBuilder.select().from(tableName), this.asInstanceOf[Group].fromRowWithAvatar)
}

object Group extends Group with TableOps {
  def insertEntity(entity: models.Group)(implicit session: Session): Future[ResultSet] =
    insert
      .value(_.id, entity.id)
      .value(_.creatorUserId, entity.creatorUserId)
      .value(_.accessHash, entity.accessHash)
      .value(_.title, entity.title)
      .future()

  def dropEntity(groupId: Int)(implicit session: Session): Future[Unit] =
    delete.where(_.id eqs groupId).future() map (_ => ())

  def getEntity(groupId: Int)(implicit session: Session): Future[Option[models.Group]] =
    select.where(_.id eqs groupId).one()

  def getEntityWithAvatar(groupId: Int)
                         (implicit session: Session): Future[Option[(models.Group, models.AvatarData)]] =
    selectWithAvatar.where(_.id eqs groupId).one()

  def updateTitle(id: Int, title: String)(implicit session: Session): Future[ResultSet] =
    update.where(_.id eqs id).modify(_.title setTo title).future()

  def updateAvatar(id: Int, avatar: models.Avatar)(implicit session: Session) =
    update.where(_.id eqs id)
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

  def removeAvatar(id: Int)(implicit session: Session) =
    updateAvatar(id, models.Avatar(None, None, None))
}
