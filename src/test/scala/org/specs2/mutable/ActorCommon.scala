package org.specs2.mutable

import akka.actor.ActorRef
import akka.testkit.{ TestKitBase, TestProbe }
import com.datastax.driver.core.{ Session => CSession }
import com.secretapp.backend.models
import com.secretapp.backend.services.common.RandomService

trait ActorCommon { self: RandomService =>
  implicit val csession: CSession

  case class SessionIdentifier(id: Long)
  object SessionIdentifier {
    def apply(): SessionIdentifier = SessionIdentifier(rand.nextLong())
  }

  case class TestScopeNew(probe: TestProbe, apiActor: ActorRef, session: SessionIdentifier, authId: Long, userOpt: Option[models.User] = None) {
    lazy val user = userOpt.get
  }
}
