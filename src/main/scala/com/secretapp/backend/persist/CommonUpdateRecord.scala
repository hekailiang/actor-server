package com.secretapp.backend.persist

import com.datastax.driver.core.ConsistencyLevel
import com.newzly.phantom.query.ExecutableStatement
import com.secretapp.backend.data.message.update.CommonUpdate
import com.secretapp.backend.data.message.{ update => updateProto }
import com.datastax.driver.core.{ ResultSet, Row, Session }
import com.newzly.phantom.Implicits._
import com.secretapp.backend.protocol.codecs.message.update.CommonUpdateMessageCodec
import com.gilt.timeuuid._
import java.util.UUID
import scala.collection.immutable
import scala.collection.JavaConversions._
import scala.concurrent.Future
import scodec.bits._
import scodec.codecs.{ uuid => uuidCodec }

sealed class CommonUpdateRecord extends CassandraTable[CommonUpdateRecord, Entity[UUID, updateProto.CommonUpdateMessage]]{
  override lazy val tableName = "common_updates"

  object authId extends LongColumn(this) with PartitionKey[Long] {
    override lazy val name = "auth_id"
  }
  object uuid extends TimeUUIDColumn(this) with PrimaryKey[UUID]
  object userIds extends SetColumn[CommonUpdateRecord, Entity[UUID, updateProto.CommonUpdateMessage], Int](this) {
    override lazy val name = "user_ids"
  }

  object updateId extends IntColumn(this) {
    override lazy val name = "update_id"
  }

  /**
    * UpdateMessage
    */
  object senderUID extends IntColumn(this) {
    override lazy val name = "sender_uid"
  }
  object destUID extends IntColumn(this) {
    override lazy val name = "dest_uid"
  }
  object mid extends IntColumn(this)
  object destKeyHash extends LongColumn(this) {
    override lazy val name = "dest_key_hash"
  }
  object useAesKey extends BooleanColumn(this) {
    override lazy val name = "use_aes_key"
  }
  // TODO: Blob type when phanton implements its support in upcoming release
  object aesKeyHex extends OptionalStringColumn(this) {
    override lazy val name = "aes_key_hex"
  }
  object message extends StringColumn(this)

  /**
    * UpdateMessageSent
    */
  // mid is already defined above

  object randomId extends LongColumn(this) {
    override lazy val name = "random_id"
  }

  /**
    * NewDevice
    */

  object newDeviceUid extends IntColumn(this) {
    override lazy val name = "NewDevice_uid"
  }
  object newDevicePublicKeyHash extends LongColumn(this) {
    override lazy val name = "NewDevice_public_key_hash"
  }

  /**
    * NewYourDevice
    */

  object newYourDeviceUid extends IntColumn(this) {
    override lazy val name = "NewYourDevice_uid"
  }
  object newYourDevicePublicKeyHash extends LongColumn(this) {
    override lazy val name = "NewYourDevice_public_key_hash"
  }
  object newYourDevicePublicKey extends BigIntColumn(this) {
    override lazy val name = "NewYourDevice_public_key"
  }

  override def fromRow(row: Row): Entity[UUID, updateProto.CommonUpdateMessage] = {
    updateId(row) match {
      case 1L =>
        Entity(uuid(row),
            updateProto.Message(senderUID(row), destUID(row), mid(row), destKeyHash(row), useAesKey(row),
              aesKeyHex(row) map (x => BitVector.fromHex(x).get), BitVector.fromHex(message(row)).get)
          )
      case 2L =>
        Entity(uuid(row),
          updateProto.NewDevice(newDeviceUid(row), newDevicePublicKeyHash(row))
        )
      case 3L =>
        Entity(uuid(row),
          updateProto.NewYourDevice(
            newYourDeviceUid(row),
            newYourDevicePublicKeyHash(row),
            BitVector(newYourDevicePublicKey(row).toByteArray)
          )
        )
      case 4L =>
        Entity(uuid(row),
            updateProto.MessageSent(mid(row), randomId(row))
          )
    }

  }
}

object CommonUpdateRecord extends CommonUpdateRecord with DBConnector {
  //import com.datastax.driver.core.querybuilder._
  //import com.newzly.phantom.query.QueryCondition

  // TODO: limit by size, not rows count
  def getDifference(authId: Long, state: Option[UUID], limit: Int = 500)
    (implicit session: Session): Future[immutable.Seq[Entity[UUID, updateProto.CommonUpdateMessage]]] = {
    //select.where(c => QueryCondition(QueryBuilder.gte(c.uuid.name, QueryBuilder.fcall("maxTimeuuid")))).limit(limit)
    //  .fetch
    val query = state match {
      case Some(uuid) =>
        CommonUpdateRecord.select.orderBy(_.uuid.asc)
          .where(_.authId eqs authId).and(_.uuid gt uuid)
      case None =>
        CommonUpdateRecord.select.orderBy(_.uuid.asc)
          .where(_.authId eqs authId)
    }
    val queryString = query.queryString
    query.limit(limit).fetch map (_.toList)
  }

  def getState(authId: Long)(implicit session: Session): Future[Option[UUID]] =
    CommonUpdateRecord.select(_.uuid).where(_.authId eqs authId).orderBy(_.uuid.desc).one

  def push(authId: Long, update: updateProto.CommonUpdateMessage)(implicit session: Session): Future[UUID] = {
    val uuid = TimeUuid()
    push(uuid, authId, update)
  }

  def push(uuid: UUID, authId: Long, update: updateProto.CommonUpdateMessage)(implicit session: Session): Future[UUID] = {
    val q = update match {
      case updateProto.Message(senderUID, destUID, mid, keyHash, useAesKey, aesKey, message) =>
        val userIds = Set(senderUID, destUID)
        insert.value(_.authId, authId).value(_.uuid, uuid)
        .value(_.userIds, userIds).value(_.updateId, 1)
        .value(_.senderUID, senderUID).value(_.destUID, destUID)
        .value(_.mid, mid).value(_.destKeyHash, keyHash).value(_.useAesKey, useAesKey).value(_.aesKeyHex, aesKey map (_.toHex))
        .value(_.message, message.toHex)
      case updateProto.MessageSent(mid, randomId) =>
        insert.value(_.authId, authId).value(_.uuid, uuid)
        .value(_.userIds, Set[Int]()).value(_.updateId, 4).value(_.mid, mid)
        .value(_.randomId, randomId)
      case updateProto.NewDevice(uid, publicKeyHash) =>
        insert.value(_.authId, authId).value(_.uuid, uuid)
          .value(_.newDeviceUid, uid).value(_.newDevicePublicKeyHash, publicKeyHash)
      case updateProto.NewYourDevice(uid, publicKeyHash, publicKey) =>
        insert.value(_.authId, authId).value(_.uuid, uuid)
          .value(_.newYourDeviceUid, uid).value(_.newYourDevicePublicKeyHash, publicKeyHash)
          .value(_.newYourDevicePublicKey, BigInt(publicKey.toByteArray))
      case _ =>
        throw new Exception("Unknown UpdateMessage")
    }

    val f = q.consistencyLevel_=(ConsistencyLevel.ALL).future

    f map (_ => uuid)
  }


}
