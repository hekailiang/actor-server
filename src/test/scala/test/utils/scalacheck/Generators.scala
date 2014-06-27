package test.utils.scalacheck

import scodec.bits.ByteVector
import org.scalacheck._
import org.scalacheck.Prop._

object Generators {

  val genLSB = for {
    n <- Gen.choose(1, 0x7f)
  } yield ByteVector(n.toByte)
  val genMSB = for {
    n <- Gen.choose(0x80, 0xff)
    tail <- genBV
  } yield ByteVector(n.toByte) ++ tail
  val genBV: Gen[ByteVector] = Gen.resize(8, Gen.oneOf(genMSB, genLSB))

}
