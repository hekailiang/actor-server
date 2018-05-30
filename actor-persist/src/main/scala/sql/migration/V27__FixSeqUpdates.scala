package sql.migration

import com.datastax.driver.core.utils.UUIDs
import com.typesafe.config._
import java.sql.Connection
import java.sql.PreparedStatement
import java.util.UUID
import org.flywaydb.core.api.migration.jdbc.JdbcMigration
import scalikejdbc._

class V27__FixSeqUpdates extends JdbcMigration {
  def migrate(connection: Connection): Unit = {
    val db: DB = new DB(connection)
    db.autoClose(false)

    db.withinTx { implicit s =>

      fixSchema()
      extractUuids()
    }
  }

  private def fixSchema()(implicit session: DBSession): Unit = {
    sql"""
      ALTER TABLE seq_updates
        ADD COLUMN ts bigint;
      ALTER TABLE seq_updates
        ADD COLUMN msb bigint;
      ALTER TABLE seq_updates
        ADD COLUMN lsb bigint;
    """.execute.apply
  }

  private def extractUuids()(implicit session: DBSession): Unit = {
    val updates = sql"select auth_id, uuid from seq_updates".map { rs =>
      (
        rs.long("auth_id"),
        UUID.fromString(rs.string("uuid"))
      )
    }.list.apply()

    updates foreach {
      case (authId, uuid) =>
        val ts = uuid.timestamp()
        val msb = uuid.getMostSignificantBits()
        val lsb = uuid.getLeastSignificantBits()

        sql"""
        update seq_updates
          set ts = ${ts}, msb = ${msb}, lsb = ${lsb}
          where auth_id = ${authId} and uuid = ${uuid}
        """.update.apply
    }

    sql"""
      ALTER TABLE seq_updates
        ALTER COLUMN ts SET NOT NULL;
      ALTER TABLE seq_updates
        ALTER COLUMN msb SET NOT NULL;
      ALTER TABLE seq_updates
        ALTER COLUMN lsb SET NOT NULL;
    """.execute.apply

    sql"""
    CREATE UNIQUE INDEX idx_seq_updates_auth_id_ts on seq_updates (auth_id, ts)
    """.execute.apply
  }
}
