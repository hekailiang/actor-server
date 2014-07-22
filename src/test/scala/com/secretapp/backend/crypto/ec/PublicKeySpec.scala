package com.secretapp.backend.crypto.ec

import org.bouncycastle.jce.provider.BouncyCastleProvider
import org.specs2.mutable.Specification
import scodec.bits.BitVector
import java.security.{Security, SecureRandom, KeyPairGenerator}
import org.bouncycastle.jce.ECNamedCurveTable

class PublicKeySpec extends Specification {
  Security.addProvider(new BouncyCastleProvider())

  "PublicKey" should {
    "pass prime192v1 validation for valid data" in {
      val prime192v1 = genPrimePK("prime192v1")
      PublicKey.isPrime192v1(prime192v1) must beTrue
    }

    "fail prime192v1 validation for invalid data" in {
      val prime192v2 = genPrimePK("prime192v2")
      PublicKey.isPrime192v1(prime192v2) must beFalse
    }
  }

  def genPrimePK(alg: String): BitVector = {
    val ecSpec = ECNamedCurveTable.getParameterSpec(alg)
    val g = KeyPairGenerator.getInstance("ECDSA", "BC")
    g.initialize(ecSpec, new SecureRandom())
    val pair = g.generateKeyPair()
    BitVector(pair.getPublic.getEncoded)
  }
}
