package com.secretapp.backend.crypto.ec

import com.secretapp.backend.protocol.codecs.ByteConstants._
import scodec.bits.BitVector
import java.security.MessageDigest

object PublicKey {
  def isPrime192v1(buf: BitVector): Boolean = buf.length == 192

  def keyHash(pk: BitVector): Long = {
    val digest = MessageDigest.getInstance("SHA-256")
    val buf = BitVector(digest.digest(pk.toByteArray))
    buf.take(longSize).toLong() ^ buf.drop(longSize).take(longSize).toLong() ^
      buf.drop(longSize * 2).take(longSize).toLong() ^ buf.drop(longSize * 3).take(longSize).toLong()
  }
}
