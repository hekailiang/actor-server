package com.secretapp.backend.crypto.ec

import java.security.{Security, SecureRandom, KeyPairGenerator}
import org.bouncycastle.jce.ECNamedCurveTable
import org.bouncycastle.jce.interfaces.ECPublicKey
import org.bouncycastle.jce.provider.BouncyCastleProvider
import org.bouncycastle.jce.spec.ECNamedCurveParameterSpec
import org.specs2.mutable.Specification
import scodec.bits._

class PublicKeySpec extends Specification {
  Security.addProvider(new BouncyCastleProvider())

  "PublicKey" should {
    "pass prime192v1 validation for valid data" in {
      val prime192v1 = genPublicKey
      PublicKey.isPrime192v1(prime192v1) must beTrue
    }

    /* "fail prime192v1 validation for invalid data" in {
      val prime192v2 = genPublicKey ++ hex"ac1d".bits
      PublicKey.isPrime192v1(prime192v2) must beFalse
    } */
  }

  def genPublicKey: BitVector = {
    val ecSpec: ECNamedCurveParameterSpec = ECNamedCurveTable.getParameterSpec("prime192v1")
    val g = KeyPairGenerator.getInstance("ECDSA", "BC")
    g.initialize(ecSpec, new SecureRandom())
    val pair = g.generateKeyPair()
    val pubKey = pair.getPublic.asInstanceOf[ECPublicKey]
    BitVector(pubKey.getQ.getEncoded)
  }
}
