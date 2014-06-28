//package com.secretapp.backend.protocol.codecs
//
//import scodec.bits.BitVector
//
//object Longs {
//  def encode(n: Long): BitVector = BitVector.fromLong(n)
//
//  def decode(buf: BitVector): Long = buf.toLong()
//
//  def encodeL(xs: Array[Long]): BitVector = {
//    val size = VarInt.encode(xs.length)
//    xs.map(encode(_)).foldLeft(size)(_ ++ _)
//  }
//
//  private def decodeL(buf: BitVector): Array[Long] = buf.grouped(64).map(decode(_)).toArray
//
//  def take(buf: BitVector): (Array[Long], BitVector) = {
//    val (len, xs) = VarInt.take(buf)
//    Tuple2(decodeL(xs.take(64 * len)), xs.drop(64 * len))
//  }
//}
