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


//  import VarString._
//  val vsres = VarString.encode("wow")
//  println(s"vsres encode: $vsres")
//  println(s"vsres decode: ${VarString.decode(vsres.toOption.get)}")

}
