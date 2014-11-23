package utils

import org.bouncycastle.jce.provider.BouncyCastleProvider
import scodec.bits.BitVector
import java.security.{ Security, MessageDigest }

object ByteConstants {

  val byteSize = 8 // bits
  val longSize = 8 * byteSize // 8 bytes to bits
  val intSize = 4 * byteSize // 4 bytes to bits
  val crcByteSize = 4 // bytes

}

object PublicKey {

  Security.addProvider(new BouncyCastleProvider())

  def keyHash(pk: BitVector): Long = {
    import utils.ByteConstants._

    val digest = MessageDigest.getInstance("SHA-256")
    val buf = BitVector(digest.digest(pk.toByteArray))
    buf.take(longSize).toLong() ^ buf.drop(longSize).take(longSize).toLong() ^
      buf.drop(longSize * 2).take(longSize).toLong() ^ buf.drop(longSize * 3).take(longSize).toLong()
  }

}
