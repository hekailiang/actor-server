package com.secretapp.backend.helpers

import akka.actor._
import akka.pattern.ask
import com.secretapp.backend.api.SocialProtocol
import scala.collection.immutable
import scala.concurrent.duration._
import scala.concurrent.Future

trait SocialHelpers extends UserHelpers {
  import SocialProtocol._

  val context: ActorContext
  val socialBrokerRegion: ActorRef

  import context.dispatcher

  def getRelations(userId: Int): Future[immutable.Set[Int]] = {
    ask(socialBrokerRegion, SocialMessageBox(userId, GetRelations))(5.seconds).mapTo[RelationsType]
  }

  def getRelatedAuthIds(userId: Int): Future[Seq[Long]] = {
    getRelations(userId) flatMap { relations =>
      Future.sequence(
        relations map getAuthIds
      )
    } map (_.toSeq.flatten)
  }

  def withRelatedAuthIds(userId: Int)(f: Seq[Long] => Any): Future[Any] = {
    for {
      authIds <- getRelatedAuthIds(userId)
    } yield {
      f(authIds)
    }
  }
}
