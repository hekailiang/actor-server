package com.secretapp.backend.protocol.codecs.message

import scala.collection.immutable.Seq
import com.secretapp.backend.data.transport._
import com.secretapp.backend.data.message._
import scodec.bits._
import scalaz._
import Scalaz._
import org.specs2.mutable._

class ContainerCodecSpec extends Specification {
  "container encode" should {
    "pack messages" in {
      val res = hex"000000000000000109010000000000000005000000000000000109010000000000000005000000000000000109010000000000000005".bits
      val ping = MessageBox(1L, Ping(5L))
      val c = Container(Seq(ping, ping, ping))
      ContainerCodec.encode(c) must beEqualTo(res.right)
    }

    "raise error if it have nested container detected" in {
      val res = "container can't be nested"
      val ping = MessageBox(1L, Ping(5L))
      val nested = MessageBox(1L, Container(Seq(ping)))
      val c = Container(Seq(ping, ping, nested))
      ContainerCodec.encode(c) must beEqualTo(res.left)
    }
  }

  "container decode" should {
    "decode binary to original container" in {
      val stream = hex"000000000000000109010000000000000005000000000000000109010000000000000005000000000000000109010000000000000005".bits
      val ping = MessageBox(1L, Ping(5L))
      val res = Container(Seq(ping, ping, ping))
      ContainerCodec.decode(stream) must beEqualTo((BitVector.empty, res).right)
    }

    "raise error if it have nested container detected" in {
      val stream = hex"0000000000000001090100000000000000050000000000000001090100000000000000050000000000000001130a000000000000000109010000000000000005".bits
      val res = "container can't be nested"
      ContainerCodec.decode(stream) must beEqualTo(res.left)
    }
  }
}
