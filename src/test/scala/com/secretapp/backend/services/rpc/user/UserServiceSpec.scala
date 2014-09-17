package com.secretapp.backend.services.rpc.user

import java.nio.file.{Files, Paths}
import scala.util.Random
import com.newzly.util.testing.AsyncAssertionsHelper._
import com.secretapp.backend.data.message.rpc.file.FileLocation
import com.secretapp.backend.data.message.rpc.user.{ResponseAvatarUploaded, RequestSetAvatar}
import com.secretapp.backend.persist.FileRecord
import com.secretapp.backend.services.rpc.RpcSpec

class UserServiceSpec extends RpcSpec {

  import system.dispatcher

  val fr = new FileRecord

  "profile service" should {

    "respond to RequestSetAvatar" in {
      implicit val scope = TestScope()

      val bytes = Files.readAllBytes(Paths.get(getClass.getResource("/avatar.jpg").toURI))
      bytes must have size 112527

      val fileId = 42
      val fileSalt = (new Random).nextString(30)

      val ffl = for (
        _    <- fr.createFile(fileId, fileSalt);
        _    <- fr.write(fileId, 0, bytes);
        hash <- fr.getAccessHash(42);
        fl    = FileLocation(42, hash)
      ) yield fl

      RequestSetAvatar(ffl.sync()) :~> <~:[ResponseAvatarUploaded]
    }
  }
}
