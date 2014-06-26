package com.secretapp.backend

import protocol.types._

object Main extends App {

  val xs = List(0x96, 0x1).map(_.toByte)
  println(VarInt.take(xs))
  println(VarInt.encode(VarInt.take(xs)._1).mkString(","))
  println(VarInt.encode(1234).mkString(","))
  println(VarInt.encode(2435).mkString(","))

//  Benchmark
//  for (n <- 1 until 100000000) {
//    val xs = VarInt.encode(n)
//    if (VarInt.take(xs)._1 != n) throw new Throwable("what?")
//  }

//  println(Longs.toByteArray(2435L).mkString(","))
//  println(s"MaxValue: ${Longs.toByteArray(scala.Long.MaxValue).toList.map(_.toInt).mkString(",")}")
//  println(s"MinValue: ${Longs.toByteArray(scala.Long.MinValue).toList.map(_.toInt).mkString(",")}")

  val longs = Longs.encodeL(List(2435L, 12L, 323234L))
  println(longs)
  println(Longs.take(longs)._1)


  val bytes = Bytes.encode(List(120, 12, -35))
  println(bytes)
  println(Bytes.take(bytes)._1)


  val str = String.encode("wowюникод∂Ω≈ç√∫")
  println(str)
  println(String.take(str)._1)

  import protocol._
  import protocol.types._
  val m = ResponseAuth(34L)
  val p = Package.build(0L, 0L, 1L, m)
  println(s"Package.encode(p): ${p}")

}
