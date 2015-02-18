package sql.migration

import java.sql.Connection
import java.sql.PreparedStatement
import org.flywaydb.core.api.migration.jdbc.JdbcMigration
import scalikejdbc._

class V8__CreateSeqUpdates extends JdbcMigration {
  def migrate(connection: Connection): Unit = {
    val db: DB = new DB(connection)
    db.autoClose(false)

    if (connection.getMetaData().getDriverName().startsWith("PostgreSQL")) {
      db.withinTx { implicit s =>
        sql"""
        CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
        """.execute.apply()
      }
    }

    db.withinTx { implicit s =>
      sql"""
      CREATE TABLE seq_updates (
        auth_id bigint NOT NULL,
        uuid uuid NOT NULL,
        header int NOT NULL,
        protobuf_data bytea NOT NULL,
        PRIMARY KEY (auth_id, uuid)
      );
      """.execute.apply
    }
  }
}
