package com.secretapp.backend

import java.net.InetSocketAddress
import akka.actor.{ ActorSystem, Props }
import akka.io.{ IO, Tcp }
import Tcp._

object Main extends App {

//  implicit val system = ActorSystem("secret-api-server")
//
//  implicit val service = system.actorOf(Props[Server], "api-service")
//
//  IO(Tcp) ! Bind(service, new InetSocketAddress("0.0.0.0", 8080))

  import protocol._
  import codecs._
  import scodec.bits._
  import scodec.{ Codec, DecodingContext }
  import scodec.codecs._
  import shapeless._

  val res = VarInt.decode(hex"9601".bits)
  println(s"res: $res")


  val a = Array[Long](6L, 1239L, Long.MaxValue)
  val lres = Longs.encode(a)
  println(s"lres encode: $lres")
  println(s"lres decode: ${Longs.decode(lres.toOption.get).toOption.get._2.mkString(",")}")
  println(s"lres decode length: ${Longs.decode(lres.toOption.get).toOption.get._2.length}")

}
