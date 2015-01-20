package com.secretapp.backend.persist

import org.specs2.matcher.ThrownExpectations
import org.specs2.mutable._
import org.specs2.specification.{ Fragments, Step }

trait CassandraSpecification extends SpecificationLike with ThrownExpectations {
  private def createDB() {

  }

  private def cleanDB() {

  }

  override def map(fs: => Fragments) = Step(createDB) ^ super.map(fs) ^ Step(cleanDB)
}
