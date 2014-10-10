package com.secretapp.backend.services.rpc.user

import java.nio.file.{Files, Paths}
import com.secretapp.backend.data.message.rpc.update.{Difference, RequestGetDifference, ResponseSeq}
import com.secretapp.backend.data.message.rpc.messaging._
import com.secretapp.backend.data.message.struct.AvatarImage
import com.secretapp.backend.data.message.update._
import com.secretapp.backend.data.models.User
import org.specs2.specification.BeforeExample
import scala.collection.immutable
import scala.util.Random
import scodec.bits._
import com.newzly.util.testing.AsyncAssertionsHelper._
import com.secretapp.backend.data.message.rpc.file.FileLocation
import com.secretapp.backend.data.message.rpc.user.{ResponseAvatarChanged, RequestEditAvatar}
import com.secretapp.backend.persist.{UserRecord, FileRecord}
import com.secretapp.backend.services.rpc.RpcSpec

class UserServiceSetAvatarSpec extends RpcSpec with BeforeExample {
/*
  "test avatar" should {
    "have proper size" in {
      origBytes must have size 112527
    }
  }
 */
  "user service on receiving `RequestSetAvatar`" should {
    /*
    "respond with `ResponseAvatarUploaded`" in {
      val r = setAvatarShouldBeOk

      r.avatar.fullImage.get.width          should_== origDimensions._1
      r.avatar.fullImage.get.height         should_== origDimensions._2
      r.avatar.fullImage.get.fileSize       should_== origBytes.length
      dbImageBytes(r.avatar.fullImage.get)  should_== origBytes

      r.avatar.smallImage.get.width         should_== smallDimensions._1
      r.avatar.smallImage.get.height        should_== smallDimensions._2
      r.avatar.smallImage.get.fileSize      should_== smallBytes.length
      dbImageBytes(r.avatar.smallImage.get) should_== smallBytes

      r.avatar.largeImage.get.width         should_== largeDimensions._1
      r.avatar.largeImage.get.height        should_== largeDimensions._2
      r.avatar.largeImage.get.fileSize      should_== largeBytes.length
      dbImageBytes(r.avatar.largeImage.get) should_== largeBytes
    }

    "update user avatar" in {
      setAvatarShouldBeOk

      dbUser.avatar       should beSome
      dbAvatar.fullImage  should beSome
      dbAvatar.smallImage should beSome
      dbAvatar.largeImage should beSome
    }

    "store full image in user avatar" in {
      setAvatarShouldBeOk

      dbFullImage.width         should_== origDimensions._1
      dbFullImage.height        should_== origDimensions._2
      dbFullImage.fileSize      should_== origBytes.length
      dbImageBytes(dbFullImage) should_== origBytes
    }

    "store large image in user avatar" in {
      setAvatarShouldBeOk

      dbLargeImage.width         should_== largeDimensions._1
      dbLargeImage.height        should_== largeDimensions._2
      dbLargeImage.fileSize      should_== largeBytes.length
      dbImageBytes(dbLargeImage) should_== largeBytes
    }

    "store small image in user avatar" in {
      setAvatarShouldBeOk

      dbSmallImage.width         should_== smallDimensions._1
      dbSmallImage.height        should_== smallDimensions._2
      dbSmallImage.fileSize      should_== smallBytes.length
      dbImageBytes(dbSmallImage) should_== smallBytes
    }
     */
    "append update to chain" in {
      val (scope1, scope2) = TestScope.pair(1, 2)

      val diff1 = {
        implicit val scope = scope1
        RequestGetDifference(0, None) :~> <~:[Difference]
      }._1

      {
        implicit val scope = scope2
        connectWithUser(scope1.user)
        setAvatarShouldBeOk
      }

      Thread.sleep(1000)

      val (diff2, updates2) = {
        implicit val scope = scope1
        RequestGetDifference(diff1.seq, diff1.state) :~> <~:[Difference]
      }

      updates2.length should beEqualTo(2)
      updates2.last.body.asInstanceOf[SeqUpdate].body should beAnInstanceOf[AvatarChanged]

      val a = diff2.users.filter(_.uid == scope2.user.uid)(0).avatar.get

      a.fullImage.get.width          should_== origDimensions._1
      a.fullImage.get.height         should_== origDimensions._2
      a.fullImage.get.fileSize       should_== origBytes.length
      dbImageBytes(a.fullImage.get)  should_== origBytes

      a.smallImage.get.width         should_== smallDimensions._1
      a.smallImage.get.height        should_== smallDimensions._2
      a.smallImage.get.fileSize      should_== smallBytes.length
      dbImageBytes(a.smallImage.get) should_== smallBytes

      a.largeImage.get.width         should_== largeDimensions._1
      a.largeImage.get.height        should_== largeDimensions._2
      a.largeImage.get.fileSize      should_== largeBytes.length
      dbImageBytes(a.largeImage.get) should_== largeBytes
    }
  }

  import system.dispatcher

  implicit val timeout = 5.seconds

  private implicit var scope: TestScope = _
  private var fl: FileLocation = _

  override def before = {
    scope = TestScope()
    fl = storeOrigImage
  }

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

  private def setAvatarShouldBeOk(implicit scope: TestScope) = {
    val (rsp, _) = RequestEditAvatar(fl) :~> <~:[ResponseAvatarChanged]
    rsp
  }

  private def dbUser =
    UserRecord.getEntity(scope.user.uid, scope.user.authId).sync().get

  private def dbAvatar = dbUser.avatar.get
  private def dbFullImage = dbAvatar.fullImage.get
  private def dbLargeImage = dbAvatar.largeImage.get
  private def dbSmallImage = dbAvatar.smallImage.get

  private def dbImageBytes(a: AvatarImage)(implicit scope: TestScope) =
    fr.getFile(a.fileLocation.fileId.toInt).sync()

  private def connectWithUser(u: User)(implicit scope: TestScope) = {
    val rq = RequestSendMessage(
      u.uid,
      u.accessHash(scope.user.authId),
      555L,
      message = EncryptedRSAMessage(
        encryptedMessage = BitVector(1, 2, 3),
        keys = immutable.Seq(
          EncryptedAESKey(
            u.publicKeyHash, BitVector(1, 0, 1, 0)
          )
        ),
        ownKeys = immutable.Seq.empty
      )
    )

    rq :~> <~:[ResponseSeq]
  }
}
