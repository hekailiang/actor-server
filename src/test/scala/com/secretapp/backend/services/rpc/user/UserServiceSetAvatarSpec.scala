package com.secretapp.backend.services.rpc.user

import java.nio.file.{Files, Paths}
import com.secretapp.backend.data.message.rpc.update.{Difference, RequestGetDifference, ResponseSeq}
import com.secretapp.backend.data.message.rpc.messaging._
import com.secretapp.backend.data.message.struct.{ AvatarImage, FileLocation }
import com.secretapp.backend.data.message.update._
import com.secretapp.backend.data.models.User
import org.specs2.specification.BeforeExample
import scala.collection.immutable
import scala.util.Random
import scodec.bits._
import com.websudos.util.testing.AsyncAssertionsHelper._
import com.secretapp.backend.data.message.rpc.user.{ResponseAvatarChanged, RequestEditAvatar}
import com.secretapp.backend.persist.{UserRecord, FileRecord}
import com.secretapp.backend.services.rpc.RpcSpec

class UserServiceSetAvatarSpec extends RpcSpec with BeforeExample {

  "valid avatar" should {
    "have proper size" in {
      validOrigBytes must have size 112527
    }
  }

  "user service on receiving `RequestSetAvatar`" should {

    "respond with `ResponseAvatarUploaded`" in {
      val r = setValidAvatarShouldBeOk

      r.avatar.fullImage.get.width          should_== validOrigDimensions._1
      r.avatar.fullImage.get.height         should_== validOrigDimensions._2
      r.avatar.fullImage.get.fileSize       should_== validOrigBytes.length
      dbImageBytes(r.avatar.fullImage.get)  should_== validOrigBytes

      r.avatar.smallImage.get.width         should_== validSmallDimensions._1
      r.avatar.smallImage.get.height        should_== validSmallDimensions._2
      r.avatar.smallImage.get.fileSize      should_== validSmallBytes.length
      dbImageBytes(r.avatar.smallImage.get) should_== validSmallBytes

      r.avatar.largeImage.get.width         should_== validLargeDimensions._1
      r.avatar.largeImage.get.height        should_== validLargeDimensions._2
      r.avatar.largeImage.get.fileSize      should_== validLargeBytes.length
      dbImageBytes(r.avatar.largeImage.get) should_== validLargeBytes
    }

    "update user avatar" in {
      setValidAvatarShouldBeOk

      dbUser.avatar       should beSome
      dbAvatar.fullImage  should beSome
      dbAvatar.smallImage should beSome
      dbAvatar.largeImage should beSome
    }

    "store full image in user avatar" in {
      setValidAvatarShouldBeOk

      dbFullImage.width         should_== validOrigDimensions._1
      dbFullImage.height        should_== validOrigDimensions._2
      dbFullImage.fileSize      should_== validOrigBytes.length
      dbImageBytes(dbFullImage) should_== validOrigBytes
    }

    "store large image in user avatar" in {
      setValidAvatarShouldBeOk

      dbLargeImage.width         should_== validLargeDimensions._1
      dbLargeImage.height        should_== validLargeDimensions._2
      dbLargeImage.fileSize      should_== validLargeBytes.length
      dbImageBytes(dbLargeImage) should_== validLargeBytes
    }

    "store small image in user avatar" in {
      setValidAvatarShouldBeOk

      dbSmallImage.width         should_== validSmallDimensions._1
      dbSmallImage.height        should_== validSmallDimensions._2
      dbSmallImage.fileSize      should_== validSmallBytes.length
      dbImageBytes(dbSmallImage) should_== validSmallBytes
    }

    "append update to chain" in {
      val (scope1, scope2) = TestScope.pair(1, 2)
      catchNewSession(scope1)
      catchNewSession(scope2)

      val diff1 = {
        implicit val scope = scope1
        RequestGetDifference(0, None) :~> <~:[Difference]
      }._1

      {
        implicit val scope = scope2
        connectWithUser(scope1.user)
        setValidAvatarShouldBeOk
      }

      Thread.sleep(1000)

      val (diff2, updates2) = {
        implicit val scope = scope1
        RequestGetDifference(diff1.seq, diff1.state) :~> <~:[Difference]
      }

      updates2.length should beEqualTo(2)
      updates2.last.body.asInstanceOf[SeqUpdate].body should beAnInstanceOf[AvatarChanged]

      val a = diff2.users.filter(_.uid == scope2.user.uid)(0).avatar.get

      a.fullImage.get.width          should_== validOrigDimensions._1
      a.fullImage.get.height         should_== validOrigDimensions._2
      a.fullImage.get.fileSize       should_== validOrigBytes.length
      dbImageBytes(a.fullImage.get)  should_== validOrigBytes

      a.smallImage.get.width         should_== validSmallDimensions._1
      a.smallImage.get.height        should_== validSmallDimensions._2
      a.smallImage.get.fileSize      should_== validSmallBytes.length
      dbImageBytes(a.smallImage.get) should_== validSmallBytes

      a.largeImage.get.width         should_== validLargeDimensions._1
      a.largeImage.get.height        should_== validLargeDimensions._2
      a.largeImage.get.fileSize      should_== validLargeBytes.length
      dbImageBytes(a.largeImage.get) should_== validLargeBytes
    }

    "respond with IMAGE_LOAD_ERROR if invalid image passed" in {
      RequestEditAvatar(invalidFileLocation) :~> <~:(400, "IMAGE_LOAD_ERROR")
    }

    "respond with FILE_TOO_BIG if huge image passed" in {
      RequestEditAvatar(tooLargeFileLocation) :~> <~:(400, "FILE_TOO_BIG")
    }
  }

  import system.dispatcher

  implicit val timeout = 5.seconds

  private implicit var scope: TestScope = _
  private var validFileLocation: FileLocation = _
  private var invalidFileLocation: FileLocation = _
  private var tooLargeFileLocation: FileLocation = _

  override def before = {
    scope = TestScope()
    catchNewSession(scope)
    validFileLocation = storeImage(42, validOrigBytes)
    invalidFileLocation = storeImage(43, invalidBytes)
    tooLargeFileLocation = storeImage(44, tooLargeBytes)
  }

  private val fr = new FileRecord

  private val validOrigBytes =
    Files.readAllBytes(Paths.get(getClass.getResource("/valid-avatar.jpg").toURI))

  private val invalidBytes = Stream.continually(Random.nextInt.toByte).take(50000).toArray

  private val tooLargeBytes =
    Files.readAllBytes(Paths.get(getClass.getResource("/too-large-avatar.jpg").toURI))

  private val validOrigDimensions = AvatarUtils.dimensions(validOrigBytes).sync()

  private val validLargeBytes = AvatarUtils.resizeToLarge(validOrigBytes).sync()
  private val validLargeDimensions = (200, 200)

  private val validSmallBytes = AvatarUtils.resizeToSmall(validOrigBytes).sync()
  private val validSmallDimensions = (100, 100)

  private def storeImage(fileId: Int, bytes: Array[Byte]): FileLocation = {
    val fileSalt = (new Random).nextString(30)

    val ffl = for (
      _    <- fr.createFile(fileId, fileSalt);
      _    <- fr.write(fileId, 0, bytes);
      hash <- fr.getAccessHash(fileId);
      fl    = FileLocation(fileId, hash)
    ) yield fl

    ffl.sync()
  }

  private def setValidAvatarShouldBeOk(implicit scope: TestScope) = {
    val (rsp, _) = RequestEditAvatar(validFileLocation) :~> <~:[ResponseAvatarChanged]
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

    Thread.sleep(1000)
  }
}
