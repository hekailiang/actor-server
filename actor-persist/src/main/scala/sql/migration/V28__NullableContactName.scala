package sql.migration

import java.sql.Connection
import java.sql.PreparedStatement
import org.flywaydb.core.api.migration.jdbc.JdbcMigration
import scalikejdbc._

class V28__NullableContactName extends JdbcMigration {
  def migrate(connection: Connection): Unit = {
    val db: DB = new DB(connection, DBConnectionAttributes(Some("postgresql")))
    db.autoClose(false)

    db.withinTx { implicit s =>
      sql"ALTER TABLE user_contacts ALTER COLUMN name DROP NOT NULL"
        .execute.apply

      sql"select owner_user_id, contact_user_id from user_contacts where name = ''"
        .map(rs => (
          rs.int("owner_user_id"),
          rs.int("contact_user_id")
        )).list.apply foreach {
        case (ownerUserId, contactUserId) =>
          sql"""
            update user_contacts set name = null
            where owner_user_id = ${ownerUserId}
            and contact_user_id = ${contactUserId}
            """.update.apply
      }
    }
  }
}
