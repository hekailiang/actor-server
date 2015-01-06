package com.secretapp.backend.persist

import akka.actor._
import akka.persistence._
import akka.serialization._
import com.secretapp.backend.models
import com.secretapp.backend.session.SessionProtocol
import com.websudos.phantom.Implicits._
import scala.collection.immutable
import scala.concurrent.Future
import scala.util.{ Failure, Success, Try }
import scodec.bits._

sealed class PersistenceMessage extends CassandraTable[PersistenceMessage, models.PersistenceMessage] {
  override val tableName = "messages"

  object processorId extends StringColumn(this) with PartitionKey[String] {
    override lazy val name = "processor_id"
  }
  object partitionNr extends LongColumn(this) with PartitionKey[Long] {
    override lazy val name = "partition_nr"
  }
  object sequenceNr extends LongColumn(this) with PrimaryKey[Long] {
    override lazy val name = "sequence_nr"
  }
  object marker extends StringColumn(this) with PrimaryKey[String]
  object message extends BlobColumn(this)

  override def fromRow(row: Row): models.PersistenceMessage =
    models.PersistenceMessage(
      processorId(row),
      partitionNr(row),
      sequenceNr(row),
      marker(row),
      BitVector(message(row))
    )
}

object PersistenceMessage extends PersistenceMessage {
  def processMessages(processorId: String, partitionNr: Long, optMarker: Option[String] = None)(f: models.PersistenceMessage => Any)(implicit session: Session) = {
    def process(sequenceNr: Long): Unit = {
      val baseQuery = select
        .where(_.processorId eqs processorId).and(_.partitionNr eqs partitionNr)
        .and(_.sequenceNr eqs sequenceNr)
      val query = optMarker match {
        case Some(marker) =>
          baseQuery.and(_.marker eqs marker)
        case None => baseQuery
      }

      query.one() map {
        case Some(message) =>
          f(message)
          process(sequenceNr + 1)
        case None =>
          println(s"stopped at $sequenceNr")
      }
    }

    process(1)
  }

  def upsertMessage(m: models.PersistenceMessage)(implicit session: Session): Future[ResultSet] =
    update
      .where(_.processorId eqs m.processorId)
      .and(_.partitionNr eqs m.partitionNr)
      .and(_.sequenceNr eqs m.sequenceNr)
      .and(_.marker eqs m.marker)
      .modify(_.message setTo m.message.toByteBuffer)
      .future

  def migrate_protobufAuthorizeUser(session: Session, akkaSession: Session)(implicit system: ActorSystem): Unit = {
    val serialization = SerializationExtension(system)

    val serializer = serialization.serializerFor(classOf[PersistentRepr])

    def migrate(m: models.PersistenceMessage): Unit = {
      def needMigrate(hex: String): Boolean = {
        val ignoreHexs = immutable.Seq(
          "7562736372696265", // ubcribe
          "5570646174657342726f6b6572", // UpdatesBroker
          "536f6369616c42726f6b6572", // SocialBroker
          "4469616c6f674d616e61676572", // DialogManager
          "547970696e67", // Typing
          "616b6b612e636f6e747269622e7061747465726e2e5368617264436f6f7264696e61746f72" // akka.contrib.pattern.ShardCoordinator
        )

        def shouldIgnore(ignoreHexs: immutable.Seq[String]): Boolean = {
          if (hex == "00")
            true
          else if (ignoreHexs.length == 0)
            false
          else {
            val ignoreHex = ignoreHexs.head

            if (hex.indexOf(ignoreHex) == -1)
              shouldIgnore(ignoreHexs.tail)
            else
              true
          }
        }

        !shouldIgnore(ignoreHexs)
      }

      val auHex = "417574686f72697a6555736572"  // AuthorizeUser
      val au1Hex = "417574686f72697a655573657231" // AuthorizeUser1

      val userHex = "6d6f64656c732f55736572" // models/User
      val user1Hex = "6d6f64656c732f5573657231" // models/User1

      val messageHex = m.message.toHex

      if (needMigrate(messageHex)) {
        val userIndex = messageHex.indexOf(userHex)

        if (userIndex >= 0) {
          Try(serializer.fromBinary(BitVector.fromHex(messageHex).get.toByteArray, Some(classOf[PersistentRepr]))) match {
            case Success(p: PersistentRepr) =>
              p.payload match {
                case SessionProtocol.AuthorizeUser(user) =>
                  UserPhone.fetchUserPhoneIds(user.uid)(session) onComplete {
                    case Success(phoneIds) =>
                      val userNew = user.toUserNew(phoneIds.toSet)

                      val newRepr = PersistentRepr(
                        payload = SessionProtocol.AuthorizeUserNew(userNew),
                        sequenceNr = p.sequenceNr,
                        persistenceId = p.persistenceId,
                        deleted = p.deleted,
                        redeliveries = p.redeliveries,
                        confirms = p.confirms,
                        confirmable = p.confirmable,
                        confirmMessage = p.confirmMessage,
                        confirmTarget = p.confirmTarget,
                        sender = p.sender
                      )

                      val newBytes = serializer.toBinary(newRepr)

                      this.synchronized {
                        println(userNew)
                        println(new String(newBytes))
                        Try(serializer.fromBinary(newBytes, Some(classOf[PersistentRepr]))) match {
                          case Success(p: PersistentRepr) =>
                            if (p.payload != SessionProtocol.AuthorizeUserNew(userNew))
                              println(s"[E] payload does not match $p")
                          case Success(x) =>
                            println(s"[E] Unknown type $x")
                          case Failure(e) =>
                            println(s"[E] Cannot deserialize serialized pr")
                            throw e
                        }
                      }

                      OldPersistenceMessage.upsertMessage(m)(akkaSession) onComplete {
                        case Success(_) =>
                          val newMessage = m.copy(message = BitVector(newBytes))
                          PersistenceMessage.upsertMessage(m.copy(message = BitVector(newBytes)))(akkaSession) onFailure {
                            case e =>
                              println(s"[E] Failed to upsert new persistent message $newMessage $e")
                          }
                        case Failure(e) =>
                          println("[E] Failed to upsert old persistent message $m $e")
                      }

                    case Failure(e) =>
                      println(s"[E] Failed to find phones for $m, $e")
                  }
                case x =>
                  println(s"[E] Payload is not AuthorizeUser $m, $p")
              }
            case Success(x) =>
              println(s"[E] Unserialized value is not PersistentRepr $x")
            case Failure(e) =>
              println(s"[E] Cannot deserialize User from $m, message: $messageHex, $e")
          }
        } else {
          println(s"[E] Cannot find models/User $m, message: $messageHex")
        }
      } else {

      }
    }

    val limit = java.lang.Integer.MAX_VALUE
    //val limit = 5000
    for (messages <- select.limit(limit).fetch()(session = akkaSession, ctx = system.dispatcher)) (messages foreach migrate)
  }
}
