package com.secretapp.backend

import java.net.InetSocketAddress
import akka.actor.{ ActorSystem, Props }
import akka.io.{ IO, Tcp }
import protocol.types._

object Main extends App {

  import Tcp._

  implicit val system = ActorSystem("secret-api-server")

  implicit val service = system.actorOf(Props[Server], "api-service")

  IO(Tcp) ! Bind(service, new InetSocketAddress("0.0.0.0", 8080))

//  import scodec.bits.ByteVector
//  import java.nio.ByteBuffer
//
//  //  Benchmark
////  for (n <- 1 until 100000000) {
////    val xs = VarInt.encode(n)
////    val res = VarInt.take(xs)._1
////    if (res != n) throw new Throwable(s"res: $res, n: $n")
////  }
//
//  val b = ByteBuffer.allocate(3)
//  b.put(5.toByte)
//  b.put(10.toByte)
//  b.put(-10.toByte)
//  b.flip
//  val v = ByteVector(b)
//  println(s"b: $b")
//  println(s"v: $v")
//
//
//  println("VarInt:")
//  val xs = ByteVector(0x96, 0x1)
//  println(s"VarInt.take: ${VarInt.take(xs)._1}")
//  println(VarInt.encode(VarInt.take(xs)._1))
//  println(VarInt.encode(1234))
//  println(VarInt.encode(2435))
//  println
//
//  val longs = Longs.encodeL(Array(2435L, 12L, 323234L))
//  println(longs)
//  println(Longs.take(longs)._1)
//
//
//  val bytes = Bytes.encode(ByteVector(120, 12, -35))
//  println(bytes)
//  println(Bytes.take(bytes)._1)
//
//
//  val str = String.encode("wowюникод∂Ω≈ç√∫")
//  println(s"String.encode: $str, ${String.take(str)._1}")
//  println(String.take(str)._1)
//
//  import protocol._
//  import protocol.types._
//  val m = ResponseAuth(34L)
//  val p = Package.build(0L, 0L, 1L, m)
//  println(s"Package.encode(p): ${p}")

}
