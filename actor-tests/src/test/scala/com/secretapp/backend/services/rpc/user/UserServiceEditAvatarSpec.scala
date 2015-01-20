package com.secretapp.backend.services.rpc.user

import com.secretapp.backend.data.message.rpc.update.{ ResponseGetDifference, RequestGetDifference, ResponseSeq }
import com.secretapp.backend.data.message.rpc.messaging._
import com.secretapp.backend.data.message.struct
import com.secretapp.backend.data.message.update._
import com.secretapp.backend.models
import com.secretapp.backend.util.{ACL, AvatarUtils}
import java.nio.file.{ Files, Paths }
import org.specs2.specification.BeforeExample
import scala.collection.immutable
import scala.util.Random
import scodec.bits._
import com.secretapp.backend.data.message.rpc.user.{ RequestEditAvatar, RequestRemoveAvatar, ResponseEditAvatar }
import com.secretapp.backend.persist
import com.secretapp.backend.services.rpc.RpcSpec
import com.websudos.util.testing._

class UserServiceEditAvatarSpec extends RpcSpec {
  "valid avatar" should {
    "have proper size" in new sqlDb {
      implicit val scope = initScopeAndStoreImages()

      validOrigBytes must have size 112527
    }
  }

  "user service on receiving `RequestSetAvatar`" should {
    "respond with `ResponseAvatarUploaded`" in new sqlDb {
      implicit val scope = initScopeAndStoreImages()

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

    "update user avatar" in new sqlDb {
      implicit val scope = initScopeAndStoreImages()

      setValidAvatarShouldBeOk

      dbAvatarData.avatar       should beSome
      dbAvatar.fullImage  should beSome
      dbAvatar.smallImage should beSome
      dbAvatar.largeImage should beSome
    }

    "store full image in user avatar" in new sqlDb {
      implicit val scope = initScopeAndStoreImages()

      setValidAvatarShouldBeOk

      dbFullImage.width         should_== validOrigDimensions._1
      dbFullImage.height        should_== validOrigDimensions._2
      dbFullImage.fileSize      should_== validOrigBytes.length
      dbImageBytes(dbFullImage) should_== validOrigBytes
    }

    "store large image in user avatar" in new sqlDb {
      implicit val scope = initScopeAndStoreImages()

      setValidAvatarShouldBeOk

      dbLargeImage.width         should_== validLargeDimensions._1
      dbLargeImage.height        should_== validLargeDimensions._2
      dbLargeImage.fileSize      should_== validLargeBytes.length
      dbImageBytes(dbLargeImage) should_== validLargeBytes
    }

    "store small image in user avatar" in new sqlDb {
      implicit val scope = initScopeAndStoreImages()

      setValidAvatarShouldBeOk

      dbSmallImage.width         should_== validSmallDimensions._1
      dbSmallImage.height        should_== validSmallDimensions._2
      dbSmallImage.fileSize      should_== validSmallBytes.length
      dbImageBytes(dbSmallImage) should_== validSmallBytes
    }

    "append update to chain" in new sqlDb {
      implicit val scope = initScopeAndStoreImages()

      val (scope1, scope2) = TestScope.pair(rand.nextInt(), rand.nextInt())
      catchNewSession(scope1)
      catchNewSession(scope2)

      val diff1 = {
        implicit val scope = scope1
        RequestGetDifference(0, None) :~> <~:[ResponseGetDifference]
      }._1

      {
        implicit val scope = scope2
        connectWithUser(scope1.user)
        setValidAvatarShouldBeOk
      }

      Thread.sleep(1000)

      val (diff2, updates2) = {
        implicit val scope = scope1
        RequestGetDifference(diff1.seq, diff1.state) :~> <~:[ResponseGetDifference]
      }

      diff2.updates.length should beEqualTo(2)
      diff2.updates.last.body.asInstanceOf[SeqUpdateMessage] should beAnInstanceOf[UserAvatarChanged]

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

    "respond with IMAGE_LOAD_ERROR if invalid image passed" in new sqlDb {
      implicit val scope = initScopeAndStoreImages()

      RequestEditAvatar(invalidFileLocation) :~> <~:(400, "IMAGE_LOAD_ERROR")
    }

    "respond with FILE_TOO_BIG if huge image passed" in new sqlDb {
      implicit val scope = initScopeAndStoreImages()

      RequestEditAvatar(tooLargeFileLocation) :~> <~:(400, "FILE_TOO_BIG")
    }
  }

  "user service on receiving `RequestSetAvatar`" should {
    "remove user avatar" in new sqlDb {
      implicit val scope = initScopeAndStoreImages()

      setValidAvatarShouldBeOk

      RequestRemoveAvatar() :~> <~:[ResponseSeq]

      dbAvatarData.avatar       should beNone
    }
  }

  import system.dispatcher

  implicit val timeout = 5.seconds

  private var validFileLocation: models.FileLocation = _
  private var invalidFileLocation: models.FileLocation = _
  private var tooLargeFileLocation: models.FileLocation = _

  def initScopeAndStoreImages() = {
    val scope = TestScope(rand.nextInt)
    catchNewSession(scope)
    validFileLocation = storeImage(42, validOrigBytes)
    invalidFileLocation = storeImage(43, invalidBytes)
    tooLargeFileLocation = storeImage(44, tooLargeBytes)
    scope
  }

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

  private def storeImage(fileId: Long, bytes: Array[Byte]): models.FileLocation = {
    val fileSalt = (new Random).nextString(30)

    val ffl = for (
      _    <- persist.File.create(fileAdapter, fileId, fileSalt);
      _    <- persist.File.write(fileAdapter, fileId, 0, bytes);
      fdOpt<- persist.FileData.find(fileId);
      fl    = models.FileLocation(fileId, ACL.fileAccessHash(fileId, fdOpt.get.accessSalt))
    ) yield fl

    ffl.sync()
  }

  private def setValidAvatarShouldBeOk(implicit scope: TestScope) = {
    val (rsp, _) = RequestEditAvatar(validFileLocation) :~> <~:[ResponseEditAvatar]
    rsp
  }

  private def dbUserAvatar(implicit scope: TestScope) =
    persist.User.findWithAvatar(scope.user.uid)(Some(scope.user.authId)).sync().get

  private def dbUser(implicit scope: TestScope) = dbUserAvatar._1
  private def dbAvatarData(implicit scope: TestScope) = dbUserAvatar._2

  private def dbAvatar(implicit scope: TestScope) = dbAvatarData.avatar.get
  private def dbFullImage(implicit scope: TestScope) = dbAvatar.fullImage.get
  private def dbLargeImage(implicit scope: TestScope) = dbAvatar.largeImage.get
  private def dbSmallImage(implicit scope: TestScope) = dbAvatar.smallImage.get

  private def dbImageBytes(a: models.AvatarImage)(implicit scope: TestScope) =
    persist.File.readAll(fileAdapter, a.fileLocation.fileId).sync()

  private def connectWithUser(u: models.User)(implicit scope: TestScope) = {
    val rq = RequestSendEncryptedMessage(
      outPeer = struct.OutPeer.privat(u.uid, ACL.userAccessHash(scope.user.authId, u)),
      555L,
      encryptedMessage = BitVector(1, 2, 3),
      keys = immutable.Seq(
        EncryptedAESKey(
          u.publicKeyHash, BitVector(1, 0, 1, 0)
        )
      ),
      ownKeys = immutable.Seq.empty
    )

    rq :~> <~:[ResponseSeqDate]

    Thread.sleep(1000)
  }
}
