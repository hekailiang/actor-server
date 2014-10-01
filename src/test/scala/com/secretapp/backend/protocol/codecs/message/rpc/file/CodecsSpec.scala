package com.secretapp.backend.protocol.codecs.message.rpc.file

import org.specs2.mutable.Specification
import com.secretapp.backend.data.message.{ RpcRequestBox, RpcResponseBox }
import com.secretapp.backend.data.message.rpc._
import com.secretapp.backend.data.message.rpc.file._
import com.secretapp.backend.protocol.codecs._
import scodec.bits._

class CodecsSpec extends Specification {
  "Codecs" should {

//    Requests

    "encode and decode RequestGetFile" in {
      val encoded = hex"031101000000100b0a04080110021000188008".bits
      val fl = FileLocation(1, 2L)
      val decoded = RpcRequestBox(Request(RequestGetFile(fl, 0, 1024)))

      protoTransportMessage.encodeValid(decoded) should_== encoded
      protoTransportMessage.decodeValidValue(encoded) should_== decoded
    }

    "encode and decode RequestUploadStart" in {
      val encoded = hex"0306010000001200".bits
      val decoded = RpcRequestBox(Request(RequestStartUpload()))

      protoTransportMessage.encodeValid(decoded) should_== encoded
      protoTransportMessage.decodeValidValue(encoded) should_== decoded
    }

    "encode and decode RequestUploadFile" in {
      val encoded = hex"031201000000140c0a040a02ac1d10011a02ac1d".bits
      val bs = hex"ac1d".bits
      val conf = UploadConfig(bs)
      val decoded = RpcRequestBox(Request(RequestUploadPart(conf, 1, bs)))

      protoTransportMessage.encodeValid(decoded) should_== encoded
      protoTransportMessage.decodeValidValue(encoded) should_== decoded
    }

    "encode and decode RequestCompleteUpload" in {
      val encoded = hex"031201000000160c0a040a02ac1d100118c0c407".bits
      val bs = hex"ac1d".bits
      val conf = UploadConfig(bs)
      val decoded = RpcRequestBox(Request(RequestCompleteUpload(conf, 1, 123456)))

      protoTransportMessage.encodeValid(decoded) should_== encoded
      protoTransportMessage.decodeValidValue(encoded) should_== decoded
    }

//    Responses

    "encode and decode ResponseFilePart" in {
      val encoded = hex"0400000000000000010a0100000011040a02ac1d".bits
      val decoded = RpcResponseBox(1L, Ok(ResponseFilePart(hex"ac1d".bits)))

      protoTransportMessage.encodeValid(decoded) should_== encoded
      protoTransportMessage.decodeValidValue(encoded) should_== decoded
    }

    "encode and decode ResponseUploadStart" in {
      val encoded = hex"0400000000000000010c0100000013060a040a02ac1d".bits
      val bs = hex"ac1d".bits
      val conf = UploadConfig(bs)
      val decoded = RpcResponseBox(1L, Ok(ResponseUploadStarted(conf)))

      protoTransportMessage.encodeValid(decoded) should_== encoded
      protoTransportMessage.decodeValidValue(encoded) should_== decoded
    }

    "encode and decode FileUploaded" in {
      val encoded = hex"0400000000000000010c0100000017060a0408011002".bits
      val location = FileLocation(1, 2L)
      val decoded = RpcResponseBox(1L, Ok(ResponseUploadCompleted(location)))

      protoTransportMessage.encodeValid(decoded) should_== encoded
      protoTransportMessage.decodeValidValue(encoded) should_== decoded
    }
  }
}
