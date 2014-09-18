package com.secretapp.backend.services.rpc.user

import java.nio.file.{Files, Paths}
import com.secretapp.backend.data.message.struct.AvatarImage
import org.specs2.specification.BeforeExample

import scala.util.Random
import com.newzly.util.testing.AsyncAssertionsHelper._
import com.secretapp.backend.data.message.rpc.file.FileLocation
import com.secretapp.backend.data.message.rpc.user.{ResponseAvatarUploaded, RequestSetAvatar}
import com.secretapp.backend.persist.{UserRecord, FileRecord}
import com.secretapp.backend.services.rpc.RpcSpec

class UserServiceSpec extends RpcSpec with BeforeExample {

  "test avatar" should {
    "have proper size" in {
      origBytes must have size 112527
    }
  }

  "profile service" should {

    "respond to `RequestSetAvatar` with `ResponseAvatarUploaded`" in {
      val fl = storeOrigImage

      setAvatarShouldBeOk(fl)
    }

    "update user avatar on receiving `RequestSetAvatar`" in {
      val fl = storeOrigImage

      setAvatarShouldBeOk(fl)

      dbUser.avatar       should beSome
      dbAvatar.fullImage  should beSome
      dbAvatar.smallImage should beSome
      dbAvatar.largeImage should beSome
    }

    "store full image in user avatar on receiving `RequestSetAvatar`" in {
      val fl = storeOrigImage

      setAvatarShouldBeOk(fl)

      dbFullImage.width         should_== origDimensions._1
      dbFullImage.height        should_== origDimensions._2
      dbFullImage.fileSize      should_== origBytes.length
      dbImageBytes(dbFullImage) should_== origBytes
    }

    "store large image in user avatar on receiving `RequestSetAvatar`" in {
      val fl = storeOrigImage

      setAvatarShouldBeOk(fl)

      dbLargeImage.width         should_== largeDimensions._1
      dbLargeImage.height        should_== largeDimensions._2
      dbLargeImage.fileSize      should_== largeBytes.length
      dbImageBytes(dbLargeImage) should_== largeBytes
    }

    "store small image in user avatar on receiving `RequestSetAvatar`" in {
      val fl = storeOrigImage

      setAvatarShouldBeOk(fl)

      dbSmallImage.width         should_== smallDimensions._1
      dbSmallImage.height        should_== smallDimensions._2
      dbSmallImage.fileSize      should_== smallBytes.length
      dbImageBytes(dbSmallImage) should_== smallBytes
    }
  }

  import system.dispatcher

  implicit val timeout = 10.seconds

  private implicit var scope: TestScope = _

  override def before = scope = TestScope()

  private val fr = new FileRecord

  private val origBytes =
    Files.readAllBytes(Paths.get(getClass.getResource("/avatar.jpg").toURI))
  private val origDimensions = AvatarUtils.dimensions(origBytes).sync()

  private val largeBytes = AvatarUtils.resizeToLarge(origBytes).sync()
  private val largeDimensions = (200, 200)

  private val smallBytes = AvatarUtils.resizeToSmall(origBytes).sync()
  private val smallDimensions = (100, 100)

  private def storeOrigImage: FileLocation = {
    val fileId = 42
    val fileSalt = (new Random).nextString(30)

    val ffl = for (
      _    <- fr.createFile(fileId, fileSalt);
      _    <- fr.write(fileId, 0, origBytes);
      hash <- fr.getAccessHash(42);
      fl    = FileLocation(42, hash)
    ) yield fl

    ffl.sync()
  }

  private def setAvatarShouldBeOk(fl: FileLocation) =
    RequestSetAvatar(fl) :~> <~:[ResponseAvatarUploaded]

  private def dbUser =
    UserRecord.getEntity(scope.user.uid, scope.user.authId).sync().get

  private def dbAvatar = dbUser.avatar.get
  private def dbFullImage = dbAvatar.fullImage.get
  private def dbLargeImage = dbAvatar.largeImage.get
  private def dbSmallImage = dbAvatar.smallImage.get

  private def dbImageBytes(a: AvatarImage) =
    fr.getFile(a.fileLocation.fileId.toInt).sync()
}
