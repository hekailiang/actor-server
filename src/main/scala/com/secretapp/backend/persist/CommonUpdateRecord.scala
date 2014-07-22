package com.secretapp.backend.persist

import com.newzly.phantom.query.ExecutableStatement
import com.secretapp.backend.data.message.update.CommonUpdate
import com.secretapp.backend.data.message.{ update => updateProto }
import com.datastax.driver.core.{ ResultSet, Row, Session }
import com.newzly.phantom.Implicits._
import com.secretapp.backend.protocol.codecs.message.UpdateMessageCodec
import com.gilt.timeuuid._
import java.util.UUID
import collection.JavaConversions._
import scala.concurrent.Future
import scodec.bits._
import scodec.codecs.{ uuid => uuidCodec }

sealed class CommonUpdateRecord extends CassandraTable[CommonUpdateRecord, Entity[(Long, UUID), CommonUpdate]]{
  override lazy val tableName = "common_updates"

  object uid extends IntColumn(this) with PartitionKey[Int] {
    override lazy val name = "uid"
  }
  object publicKeyHash extends LongColumn(this) with PartitionKey[Long] {
    override lazy val name = "public_key_hash"
  }
  object uuid extends TimeUUIDColumn(this) with PrimaryKey[UUID]
  object seq extends IntColumn(this)
  object userIds extends SetColumn[CommonUpdateRecord, Entity[(Long, UUID), CommonUpdate], Int](this) {
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
  object keyHash extends LongColumn(this) {
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

  override def fromRow(row: Row): Entity[(Long, UUID), CommonUpdate] = {
    updateId(row) match {
      case 1L =>
        Entity((publicKeyHash(row), uuid(row)),
          CommonUpdate(
            seq(row),
            uuidCodec.encodeValid(uuid(row)),
            updateProto.Message(senderUID(row), destUID(row), mid(row), keyHash(row), useAesKey(row),
              aesKeyHex(row) map (x => BitVector.fromHex(x).get), BitVector.fromHex(message(row)).get)
          ))
      case 4L =>
        Entity((publicKeyHash(row), uuid(row)),
          CommonUpdate(
            seq(row),
            uuidCodec.encodeValid(uuid(row)),
            updateProto.MessageSent(mid(row), randomId(row))
          ))
    }

  }
}

object CommonUpdateRecord extends CommonUpdateRecord with DBConnector {
  //import com.datastax.driver.core.querybuilder._
  //import com.newzly.phantom.query.QueryCondition

  // TODO: limit by size, not rows count
  def getDifference(uid: Int, pubkeyHash: Long, state: UUID, limit: Int = 500)(implicit session: Session): Future[Seq[Entity[(Long, UUID), CommonUpdate]]] = {
    //select.where(c => QueryCondition(QueryBuilder.gte(c.uuid.name, QueryBuilder.fcall("maxTimeuuid")))).limit(limit)
    //  .fetch

    CommonUpdateRecord.select.orderBy(_.uuid.asc)
      .where(_.uid eqs uid).and(_.publicKeyHash eqs pubkeyHash).and(_.uuid gt state)
      .limit(limit).fetch
  }

  def getState(uid: Int, pubkeyHash: Long)(implicit session: Session): Future[Option[UUID]] =
    CommonUpdateRecord.select(_.uuid).where(_.uid eqs uid).and(_.publicKeyHash eqs pubkeyHash).orderBy(_.uuid.desc).one

  def push(uid: Int, pubkeyHash: Long, seq: Int, update: updateProto.UpdateMessage)(implicit session: Session) = Future[UUID] {
    val uuid = TimeUuid()

    update match {
      case updateProto.Message(senderUID, destUID, mid, keyHash, useAesKey, aesKey, message) =>
        val userIds: java.util.Set[Int] = Set(senderUID, destUID)
        // TODO: Prepared statement
        session.execute("""
          INSERT INTO common_updates (
            uid, public_key_hash, uuid, seq, user_ids, update_id,
            sender_uid, dest_uid, mid, dest_key_hash, use_aes_key, aes_key_hex, message
          )
          VALUES (?, ?, ?, ?, ?, 1,
                  ?, ?, ?, ?, ?, ?, ?)
        """,
          new java.lang.Integer(uid), new java.lang.Long(pubkeyHash), uuid, new java.lang.Integer(seq), userIds,
          new java.lang.Integer(senderUID), new java.lang.Integer(destUID), new java.lang.Integer(mid),
          new java.lang.Long(keyHash), new java.lang.Boolean(useAesKey),
          aesKey map (x => x.toHex) getOrElse (null), message.toHex)
      case updateProto.MessageSent(mid, randomId) =>
        session.execute("""
          INSERT INTO common_updates (
            uid, public_key_hash, uuid, seq, user_ids, update_id,
            mid, random_id
          )
          VALUES (?, ?, ?, ?, ?, 4,
                  ?, ?)
        """,
          new java.lang.Integer(uid), new java.lang.Long(pubkeyHash), TimeUuid(), new java.lang.Integer(seq), Set[Int](): java.util.Set[Int],
          new java.lang.Integer(mid), new java.lang.Long(randomId))
    }

    uuid
  }


}
