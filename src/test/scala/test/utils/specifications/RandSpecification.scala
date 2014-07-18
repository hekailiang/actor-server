package test.utils.specifications

import scala.util.Random
import org.specs2.specification.SpecificationStructure

trait RandSpecification extends SpecificationStructure {
  val rand = new Random()
}
