package com.secretapp.backend.helpers

import akka.actor._
import com.datastax.driver.core.{ Session => CSession }
import com.googlecode.concurrentlinkedhashmap.ConcurrentLinkedHashMap
import com.secretapp.backend.data.models.User
import com.secretapp.backend.persist.UserRecord
import scala.collection.immutable
import scala.concurrent.Future
import scala.language.postfixOps

trait UserHelpers {
  val context: ActorContext
  implicit val session: CSession

  import context.dispatcher

  // Caches userId -> accessHash associations
  val usersCache = new ConcurrentLinkedHashMap.Builder[Int, immutable.Seq[(Long, User)]]
    .initialCapacity(10).maximumWeightedCapacity(100).build

  def getUsers(uid: Int): Future[Seq[(Long, User)]] = {
    Option(usersCache.get(uid)) match {
      case Some(users) =>
        Future.successful(users)
      case None =>
        UserRecord.byUid(uid) map (
          _ map {user => (user.publicKeyHash, user) }
        )
    }
  }

  // FIXME: select from C* authId only for better performance
  def getAuthIds(uid: Int): Future[immutable.Seq[Long]] = {
    for {
      users <- getUsers(uid)
    } yield {
      val authIds = users.toList map (_._2.authId)
      println(s"authIds for $uid are $authIds")
      authIds
    }
  }
}
