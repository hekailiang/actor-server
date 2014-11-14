package persist

import com.datastax.driver.core.Row
import com.websudos.phantom.Implicits._
import scala.concurrent.Future
import errors.NotFoundException.getOrNotFound

sealed class AuthSmsCode extends CassandraTable[persist.AuthSmsCode, models.AuthSmsCode] {

  override lazy val tableName = "auth_sms_codes"

  object phone extends LongColumn(this) with PartitionKey[Long] {
    override lazy val name = "phone_number"
  }

  object smsHash extends StringColumn(this) {
    override lazy val name = "sms_hash"
  }

  object smsCode extends StringColumn(this) {
    override lazy val name = "sms_code"
  }

  override def fromRow(row: Row): models.AuthSmsCode = {
    models.AuthSmsCode(phone(row), smsHash(row), smsCode(row))
  }

}

object AuthSmsCode extends AuthSmsCode with DbConnector {

  def save(c: models.AuthSmsCode): Future[models.AuthSmsCode] =
    insert
      .value(_.phone,   c.phone)
      .value(_.smsHash, c.smsHash)
      .value(_.smsCode, c.smsCode)
      .future() map (_ => c)

  def byPhone(phone: Long): Future[Option[models.AuthSmsCode]] =
    select.where(_.phone eqs phone).one()

  def getByPhone(phone: Long): Future[models.AuthSmsCode] =
    getOrNotFound(byPhone(phone))

  def remove(phone: Long): Future[Unit] =
    delete.where(_.phone eqs phone).future() map (_ => ())

  def list(startPhoneExclusive: Long, count: Int): Future[Seq[models.AuthSmsCode]] =
    select.where(_.phone gtToken startPhoneExclusive).limit(count).fetch()

  def list(count: Int): Future[Seq[models.AuthSmsCode]] =
    select.one flatMap {
      case Some(first) => select.where(_.phone gteToken first.phone).limit(count).fetch()
      case _           => Future.successful(Seq())
    }

  def list(startPhoneExclusive: Option[Long], count: Int): Future[Seq[models.AuthSmsCode]] =
    startPhoneExclusive.fold(list(count))(list(_, count))

}
