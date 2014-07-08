package com.secretapp.backend.protocol.codecs

import scodec.bits.BitVector
import java.util.zip.CRC32

package object utils {

  def encodeCRCR32(buf : BitVector) : BitVector = {
    val crc32 = new CRC32()
    crc32.update(buf.toByteArray)
    BitVector.fromInt((crc32.getValue & 0xffffffff).toInt)
  }

}
