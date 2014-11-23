package utils

import com.secretapp.backend.models.UserCreationRequest
import scodec.bits.BitVector
import scala.util.Random
import com.secretapp.backend.models
import scala.collection.immutable

object Gen {

  def genInt = Random.nextInt()

  def genLong = Random.nextLong()

  // TODO: Generate variable-sized vector
  def genBitVector = BitVector.fromLong(Random.nextLong())

  // TODO: Generate variable-sized string
  def genString = Random.nextString(10)

  def genSex = models.Sex.fromInt(Random.nextInt(3) + 1)

  def genUser = {
    val ph = genLong

    models.User(
      genInt,
      genLong,
      ph,
      genBitVector,
      genLong,
      genString,
      genString,
      "", // TODO:
      genSex,
      keyHashes = immutable.Set(ph)
    )
  }

  def genUserCreationRequest = UserCreationRequest(
    BitVector.fromLong(Random.nextLong()),
    Random.nextLong(),
    Random.nextString(10),
    genSex
  )

  def genPhone = (1 to 11).foldLeft(0L) { (s, _) => s * 10 + Random.nextInt(9) + 1 }

  def genAuthSmsCode = models.AuthSmsCode(genPhone, genString, genString)

}
