package com.secretapp.backend.api

import akka.actor._
import com.secretapp.backend.data.message.struct
import com.secretapp.backend.models
import com.secretapp.backend.persist
import com.secretapp.backend.services.rpc.RpcSpec
import com.secretapp.backend.util.{ ACL, AvatarUtils }
import im.actor.server.persist.file.adapter.FileAdapter
import java.nio.file.{ Files, Paths }
import scala.concurrent._, duration._
import scala.util.Random

trait AvatarSpecHelpers {
  this: RpcSpec =>

  protected val validOrigAvatarBytes =
    Files.readAllBytes(Paths.get(getClass.getResource("/valid-avatar.jpg").toURI))

  protected val invalidAvatarBytes = Stream.continually(Random.nextInt().toByte).take(50000).toArray

  protected val tooLargeAvatarBytes =
    Files.readAllBytes(Paths.get(getClass.getResource("/too-large-avatar.jpg").toURI))

  protected val validOrigAvatarDimensions = Await.result(
    AvatarUtils.dimensions(validOrigAvatarBytes),
    5.seconds
  )

  protected val validLargeAvatarBytes = Await.result(
    AvatarUtils.resizeToLarge(validOrigAvatarBytes),
    5.seconds
  )
  protected val validLargeAvatarDimensions = (200, 200)

  protected val validSmallAvatarBytes = Await.result(
    AvatarUtils.resizeToSmall(validOrigAvatarBytes),
    5.seconds
  )
  protected val validSmallAvatarDimensions = (100, 100)


  protected def storeAvatarFile(fa: FileAdapter, fileId: Int, bytes: Array[Byte]): models.FileLocation = {
    val fileSalt = (new Random).nextString(30)

    val ffl = for (
      _    <- persist.File.create(fa, fileId, fileSalt);
      _    <- persist.File.write(fa, fileId, 0, bytes);
      fdOpt<- persist.FileData.find(fileId);
      fl   = models.FileLocation(fileId, ACL.fileAccessHash(fileId, fdOpt.get.accessSalt))
    ) yield fl

    Await.result(ffl, 5.seconds)
  }

  protected def storeAvatarFiles(fa: FileAdapter): (
    models.FileLocation,
    models.FileLocation,
    models.FileLocation
  ) = {
    (
      storeAvatarFile(fa, 42, validOrigAvatarBytes),
      storeAvatarFile(fa, 43, invalidAvatarBytes),
      storeAvatarFile(fa, 44, tooLargeAvatarBytes)
    )
  }
}
