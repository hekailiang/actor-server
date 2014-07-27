package com.secretapp.backend.protocol.codecs.message.rpc.file

import org.specs2.mutable.Specification
import com.secretapp.backend.data.message.{ RpcRequestBox, RpcResponseBox }
import com.secretapp.backend.data.message.rpc._
import com.secretapp.backend.data.message.rpc.file._
import com.secretapp.backend.protocol.codecs._
import scodec.bits._
import com.google.protobuf.{ ByteString => ProtoByteString }

class CodecsSpec extends Specification {
  "Codecs" should {

//    Requests

    "encode and decode RequestGetFile" in {
      val encoded = hex"031101000000100b0a04080110021000188008".bits
      val fl = FileLocation(1L, 2L)
      val decoded = RpcRequestBox(Request(RequestGetFile(fl, 0, 1024)))

      protoTransportMessage.encodeValid(decoded) should_== encoded
      protoTransportMessage.decodeValidValue(encoded) should_== decoded
    }

    "encode and decode RequestUploadStart" in {
      val encoded = hex"0306010000001200".bits
      val decoded = RpcRequestBox(Request(RequestUploadStart()))

      protoTransportMessage.encodeValid(decoded) should_== encoded
      protoTransportMessage.decodeValidValue(encoded) should_== decoded
    }

    "encode and decode RequestUploadFile" in {
      val encoded = hex"031401000000140e0a050a0301020310011a03010203".bits
      val bs = ProtoByteString.copyFrom(Array[Byte](1, 2, 3))
      val conf = UploadConfig(bs)
      val decoded = RpcRequestBox(Request(RequestUploadFile(conf, 1, bs)))

      protoTransportMessage.encodeValid(decoded) should_== encoded
      protoTransportMessage.decodeValidValue(encoded) should_== decoded
    }

    "encode and decode RequestCompleteUpload" in {
      val encoded = hex"031301000000160d0a050a03010203100118c0c407".bits
      val bs = ProtoByteString.copyFrom(Array[Byte](1, 2, 3))
      val conf = UploadConfig(bs)
      val decoded = RpcRequestBox(Request(RequestCompleteUpload(conf, 1, 123456)))

      protoTransportMessage.encodeValid(decoded) should_== encoded
      protoTransportMessage.decodeValidValue(encoded) should_== decoded
    }

//    Responses

    "encode and decode ResponseFilePart" in {
      val encoded = hex"0400000000000000010b0100000011050a03010203".bits
      val bs = ProtoByteString.copyFrom(Array[Byte](1, 2, 3))
      val decoded = RpcResponseBox(1L, Ok(ResponseFilePart(bs)))

      protoTransportMessage.encodeValid(decoded) should_== encoded
      protoTransportMessage.decodeValidValue(encoded) should_== decoded
    }

    "encode and decode ResponseUploadStart" in {
      val encoded = hex"0400000000000000010d0100000013070a050a03010203".bits
      val bs = ProtoByteString.copyFrom(Array[Byte](1, 2, 3))
      val conf = UploadConfig(bs)
      val decoded = RpcResponseBox(1L, Ok(ResponseUploadStart(conf)))

      protoTransportMessage.encodeValid(decoded) should_== encoded
      protoTransportMessage.decodeValidValue(encoded) should_== decoded
    }

    "encode and decode ResponseFileUploadStarted" in {
      val encoded = hex"04000000000000000106010000001500".bits
      val decoded = RpcResponseBox(1L, Ok(ResponseFileUploadStarted()))

      protoTransportMessage.encodeValid(decoded) should_== encoded
      protoTransportMessage.decodeValidValue(encoded) should_== decoded
    }

    "encode and decode FileUploaded" in {
      val encoded = hex"0400000000000000010c0100000017060a0408011002".bits
      val location = FileLocation(1L, 2L)
      val decoded = RpcResponseBox(1L, Ok(FileUploaded(location)))

      protoTransportMessage.encodeValid(decoded) should_== encoded
      protoTransportMessage.decodeValidValue(encoded) should_== decoded
    }
  }
}
