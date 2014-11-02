package com.secretapp.backend.api.rpc

import com.secretapp.backend.data.message.rpc.{Error, RpcResponse}
import scodec.bits.BitVector
import scala.concurrent.Future
import scalaz._
import Scalaz._
import java.util.regex.Pattern

object RpcValidators {
  def nonEmptyString(s: String): \/[NonEmptyList[String], String] = {
    val trimmed = s.trim
    if (trimmed.isEmpty) "Should be nonempty".wrapNel.left else trimmed.right
  }

  def printableString(s: String): \/[NonEmptyList[String], String] = {
    val p = Pattern.compile("\\p{Print}+", Pattern.UNICODE_CHARACTER_CLASS)
    if (p.matcher(s).matches) s.right else "Should contain printable characters only".wrapNel.left
  }

  def validName(n: String): \/[NonEmptyList[String], String] =
    nonEmptyString(n).flatMap(printableString)

  def validPublicKey(k: BitVector): \/[NonEmptyList[String], BitVector] =
    if (k == BitVector.empty) "Should be nonempty".wrapNel.left else k.right

  def validationFailed(errorName: String, errors: NonEmptyList[String]): Future[RpcResponse] =
    Future.successful(Error(400, errorName, errors.toList.mkString(", "), false))

  def withValidName(n: String)
                   (f: String => Future[RpcResponse]): Future[RpcResponse] =
    validName(n).fold(validationFailed("NAME_INVALID", _), f)

  def withValidPublicKey(k: BitVector)
                        (f: BitVector => Future[RpcResponse]): Future[RpcResponse] =
    validPublicKey(k).fold(validationFailed("PUBLIC_KEY_INVALID", _), f)

}