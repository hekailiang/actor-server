//package com.secretapp.backend.protocol.codecs
//
//import scodec.bits.BitVector
//
//class Bytes(val n: BitVector) extends AnyVal {
//  override def toString = s"Bytes(${n.toString})"
//}
//
//object Bytes {
//  def encode(xs: BitVector): BitVector = VarInt.encode(xs.length / 8) ++ xs
//
//  def take(buf: BitVector): (BitVector, BitVector) = {
//    val (len, xs) = VarInt.take(buf)
//    Tuple2(xs.take(8 * len), xs.drop(8 * len))
//  }
//}
