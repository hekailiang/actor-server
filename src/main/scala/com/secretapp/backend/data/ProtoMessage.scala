//package com.secretapp.backend.data
//
//import scodec.bits._
//import scodec.{ Codec, DecodingContext }
//import scodec.codecs._
//import shapeless._
//import scalaz._
//import Scalaz._
//
//
////sealed trait RpcMessage
////sealed trait RpcRequestMessage extends RpcMessage
////sealed trait RpcResponseMessage extends RpcMessage
//
////case class SendSMSCode(phoneNumber: String, appId: Long, appHash: Long) extends RpcRequestMessage
////object SendSMSCode {
////  val header = 0x1
////}
////case class SentSMSCode(phoneRegistered: Boolean, smsCodeHash: Long) extends RpcResponseMessage
////object SentSMSCode {
////  val header = 0x2
////}
////case class SignUp(phoneNumber: String, smsCodeHash: Long, smsCode: String, firstName: String, lastName: String,
////                  sex: Sex, photo: BitVector) extends RpcRequestMessage
////object SignUp {
////  val header = 0x3
////}
////case class SignIn(phoneNumber: String, smsCodeHash: Long, smsCode: String) extends RpcRequestMessage
////object SignIn {
////  val header = 0x4
////}
//////  TODO: add user structure
////case class Authorization(expiresEpoch: Long) extends RpcResponseMessage
////object Authorization {
////  val header = 0x5
////}
//
////case class RpcRequest(message: RpcRequestMessage) extends ProtoMessage
////object RpcRequest {
////  val header = 0x3
////}
////
////case class RpcResponse(messageId: Long, message: RpcResponseMessage) extends ProtoMessage
////object RpcResponse {
////  val header = 0x4
////}
