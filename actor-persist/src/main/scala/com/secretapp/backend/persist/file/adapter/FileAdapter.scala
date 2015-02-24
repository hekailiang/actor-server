package im.actor.server.persist.file.adapter

import play.api.libs.iteratee.Enumerator
import scala.concurrent._

trait FileAdapter {
  def create(name: String): Future[Unit]

  def write(name: String, offset: Int, bytes: Array[Byte]): Future[Unit]

  def read(name: String, offset: Int, length: Int): Future[Array[Byte]]

  def read(name: String): Enumerator[Array[Byte]]

  def readAll(name: String): Future[Array[Byte]]
}
