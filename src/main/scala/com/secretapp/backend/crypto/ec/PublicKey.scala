package com.secretapp.backend.crypto.ec

import com.secretapp.backend.protocol.codecs.ByteConstants._
import org.bouncycastle.jce.ECNamedCurveTable
import org.bouncycastle.jce.spec.ECNamedCurveParameterSpec
import org.bouncycastle.math.ec.ECCurve
import org.bouncycastle.jce.provider.BouncyCastleProvider
import scodec.bits.BitVector
import java.security.{ Security, MessageDigest }

object PublicKey {
  Security.addProvider(new BouncyCastleProvider())

  def keyHash(pk: BitVector): Long = {
    val digest = MessageDigest.getInstance("SHA-256")
    val buf = BitVector(digest.digest(pk.toByteArray))
    buf.take(longSize).toLong() ^ buf.drop(longSize).take(longSize).toLong() ^
      buf.drop(longSize * 2).take(longSize).toLong() ^ buf.drop(longSize * 3).take(longSize).toLong()
  }
}
