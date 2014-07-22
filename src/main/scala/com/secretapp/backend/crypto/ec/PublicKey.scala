package com.secretapp.backend.crypto.ec

import org.bouncycastle.asn1.{ASN1ObjectIdentifier, ASN1Primitive}
import org.bouncycastle.asn1.x509.SubjectPublicKeyInfo
import org.bouncycastle.asn1.x9.{X9ObjectIdentifiers, X962Parameters}
import scodec.bits.BitVector

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
}
