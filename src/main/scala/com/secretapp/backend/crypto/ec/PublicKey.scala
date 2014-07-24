package com.secretapp.backend.crypto.ec

import com.secretapp.backend.protocol.codecs.ByteConstants._
import org.bouncycastle.asn1.{ASN1ObjectIdentifier, ASN1Primitive}
import org.bouncycastle.asn1.x509.SubjectPublicKeyInfo
import org.bouncycastle.asn1.x9.{X9ObjectIdentifiers, X962Parameters}
import scodec.bits.BitVector
import java.security.MessageDigest

object PublicKey {
  def isPrime192v1(buf: BitVector): Boolean = {
    try {
      val info = SubjectPublicKeyInfo.getInstance(ASN1Primitive.fromByteArray(buf.toByteArray))
      val params = X962Parameters.getInstance(info.getAlgorithm.getParameters)
      if (params.isNamedCurve) {
        val oid: ASN1ObjectIdentifier = params.getParameters.asInstanceOf[ASN1ObjectIdentifier]
        X9ObjectIdentifiers.prime192v1.equals(oid)
      } else false
    }
    catch {
      case _: Throwable => false
    }
  }

  def keyHash(pk: BitVector): Long = {
    val digest = MessageDigest.getInstance("SHA-256")
    val buf = BitVector(digest.digest(pk.toByteArray))
    buf.take(longSize).toLong() ^ buf.drop(longSize).take(longSize).toLong() ^
      buf.drop(longSize * 2).take(longSize).toLong() ^ buf.drop(longSize * 3).take(longSize).toLong()
  }
}
