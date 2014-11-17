package utils

import scodec.bits.BitVector
import scala.util.Random
import com.secretapp.backend.{models => m}

object Gen {

  def genInt = Random.nextInt()

  def genLong = Random.nextLong()

  // TODO: Generate variable-sized vector
  def genBitVector = BitVector.fromLong(Random.nextLong())

  // TODO: Generate variable-sized string
  def genString = Random.nextString(10)

  def genSex = m.Sex.fromInt(Random.nextInt(3) + 1)

  def genUser = m.User(
    genInt,
    genLong,
    genLong,
    genBitVector,
    genLong,
    genString,
    genString,
    genString,
    genSex
  )

  def genUserCreationRequest = models.UserCreationRequest(
    BitVector.fromLong(Random.nextLong()),
    Random.nextLong(),
    Random.nextString(10),
    genSex
  )

  def genPhone = (1 to 11).foldLeft(0L) { (s, _) => s * 10 + Random.nextInt(9) + 1 }

  def genAuthSmsCode = com.secretapp.backend.models.AuthSmsCode(genPhone, genString, genString)

}
