package com.secretapp.backend.persist

import java.util.{ Date, UUID }
import com.datastax.driver.core.Row
import com.newzly.phantom.Implicits._
import scala.math.BigInt
import com.secretapp.backend.data._
import com.secretapp.backend.data.Implicits._

sealed class UserRecord extends CassandraTable[UserRecord, Entity[Long, User]] {
  object id extends BigIntColumn(this) with PartitionKey[BigInt]
  object firstName extends StringColumn(this)
  object lastName extends StringColumn(this)
  object sex extends IntColumn(this)

  override def fromRow(row: Row): Entity[Long, User] = {
    Entity(id(row).toLong, User(firstName(row), lastName(row), sex(row)))
  }
}
