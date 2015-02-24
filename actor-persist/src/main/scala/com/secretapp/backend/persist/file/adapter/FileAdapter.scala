package im.actor.server.persist.file.adapter

import play.api.libs.iteratee.Enumerator
import scala.concurrent._

trait FileAdapter {
  def create(name: String): Future[Array[Byte]]

  def write(adapterData: Array[Byte], offset: Int, bytes: Array[Byte]): Future[Array[Byte]]

  def read(adapterData: Array[Byte], offset: Int, length: Int): Future[Array[Byte]]

  def read(adapterData: Array[Byte]): Enumerator[Array[Byte]]

  def readAll(adapterData: Array[Byte]): Future[Array[Byte]]
}
