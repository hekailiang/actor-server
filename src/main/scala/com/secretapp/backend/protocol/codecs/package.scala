package com.secretapp.backend.protocol

package object codecs {

  object ByteConstants {
    val byteSize = 8 // bits
    val longSize = 8 * byteSize // 8 bytes to bits
    val crcByteSize = 4 // bytes
  }

}
