package com.secretapp.backend.protocol.codecs.message

import scala.collection.immutable.Seq
import com.secretapp.backend.data.transport._
import com.secretapp.backend.data.message._
import scodec.bits._
import scalaz._
import Scalaz._
import org.specs2.mutable._

class ContainerCodecSpec extends Specification {
  "container" should {
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
}
