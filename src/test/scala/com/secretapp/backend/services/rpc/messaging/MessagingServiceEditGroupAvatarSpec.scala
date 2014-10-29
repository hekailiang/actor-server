package com.secretapp.backend.services.rpc.user

import java.nio.file.{ Files, Paths }
import com.secretapp.backend.data.message.rpc.update.{ Difference, RequestGetDifference }
import com.secretapp.backend.data.message.rpc.messaging._
import com.secretapp.backend.data.message.struct.{ AvatarImage, FileLocation }
import com.secretapp.backend.data.message.update._
import com.secretapp.backend.util.{ACL, AvatarUtils}
import org.specs2.specification.BeforeExample
import scala.collection.immutable
import scala.util.Random
import scodec.bits._
import com.websudos.util.testing.AsyncAssertionsHelper._
import com.secretapp.backend.data.message.rpc.ResponseAvatarChanged
import com.secretapp.backend.persist.{ GroupRecord, FileRecord }
import com.secretapp.backend.services.rpc.RpcSpec

class MessagingServiceEditGroupAvatarSpec extends RpcSpec with BeforeExample {

  "valid avatar" should {
    "have proper size" in {
      validOrigBytes must have size 112527
    }
  }

  "user service on receiving `RequestSetAvatar`" should {

    "respond with `ResponseAvatarUploaded`" in {
      val (scope1, scope2) = TestScope.pair(1, 2)
      catchNewSession(scope1)
      catchNewSession(scope2)

      val respGroup = createGroup(scope1, scope2)

      {
        implicit val scope = scope1

        val r = setValidAvatarShouldBeOk(respGroup.groupId, respGroup.accessHash)

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
    }

    "update user avatar" in {
      val (scope1, scope2) = TestScope.pair(1, 2)
      catchNewSession(scope1)
      catchNewSession(scope2)

      val respGroup = createGroup(scope1, scope2)

      val r = {
        implicit val scope = scope1

        setValidAvatarShouldBeOk(respGroup.groupId, respGroup.accessHash)
      }

      dbGroup(respGroup.groupId)._2.avatar    should beSome
      dbAvatar(respGroup.groupId).fullImage   should beSome
      dbAvatar(respGroup.groupId).smallImage  should beSome
      dbAvatar(respGroup.groupId).largeImage  should beSome
    }

    "store full image in user avatar" in {
      val (scope1, scope2) = TestScope.pair(1, 2)
      catchNewSession(scope1)
      catchNewSession(scope2)

      val respGroup = createGroup(scope1, scope2)

      {
        implicit val scope = scope1

        setValidAvatarShouldBeOk(respGroup.groupId, respGroup.accessHash)

        dbFullImage(respGroup.groupId).width           should_== validOrigDimensions._1
        dbFullImage(respGroup.groupId).height          should_== validOrigDimensions._2
        dbFullImage(respGroup.groupId).fileSize        should_== validOrigBytes.length
        dbImageBytes(dbFullImage((respGroup.groupId))) should_== validOrigBytes
      }
    }

    "store large image in user avatar" in {
      val (scope1, scope2) = TestScope.pair(1, 2)
      catchNewSession(scope1)
      catchNewSession(scope2)

      val respGroup = createGroup(scope1, scope2)

      {
        implicit val scope = scope1

        setValidAvatarShouldBeOk(respGroup.groupId, respGroup.accessHash)

        dbLargeImage(respGroup.groupId).width         should_== validLargeDimensions._1
        dbLargeImage(respGroup.groupId).height        should_== validLargeDimensions._2
        dbLargeImage(respGroup.groupId).fileSize      should_== validLargeBytes.length
        dbImageBytes(dbLargeImage(respGroup.groupId)) should_== validLargeBytes
      }
    }

    "store small image in user avatar" in {
      val (scope1, scope2) = TestScope.pair(1, 2)
      catchNewSession(scope1)
      catchNewSession(scope2)

      val respGroup = createGroup(scope1, scope2)

      {
        implicit val scope = scope1

        setValidAvatarShouldBeOk(respGroup.groupId, respGroup.accessHash)

        dbSmallImage(respGroup.groupId).width         should_== validSmallDimensions._1
        dbSmallImage(respGroup.groupId).height        should_== validSmallDimensions._2
        dbSmallImage(respGroup.groupId).fileSize      should_== validSmallBytes.length
        dbImageBytes(dbSmallImage(respGroup.groupId)) should_== validSmallBytes
      }
    }

    "append update to chain" in {
      val (scope1, scope2) = TestScope.pair(1, 2)
      catchNewSession(scope1)
      catchNewSession(scope2)

      val diff1 = {
        implicit val scope = scope1
        RequestGetDifference(0, None) :~> <~:[Difference]
      }._1

      val respGroup = {
        implicit val scope = scope1
        val respGroup = createGroup(scope1, scope2)
        setValidAvatarShouldBeOk(respGroup.groupId, respGroup.accessHash)
        respGroup
      }

      Thread.sleep(1000)

      {
        implicit val scope = scope1
        val (diff2, updates2) = RequestGetDifference(diff1.seq, diff1.state) :~> <~:[Difference]

        updates2.length should beEqualTo(1)
        val update = updates2.last.body.asInstanceOf[SeqUpdate].body.assertInstanceOf[GroupAvatarChanged]

        val a = update.avatar.get

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
    }

    "respond with IMAGE_LOAD_ERROR if invalid image passed" in {
      val (scope1, scope2) = TestScope.pair(1, 2)
      catchNewSession(scope1)
      catchNewSession(scope2)

      val respGroup = createGroup(scope1, scope2)

      {
        implicit val scope = scope1

        RequestEditGroupAvatar(respGroup.groupId, respGroup.accessHash, invalidFileLocation) :~> <~:(400, "IMAGE_LOAD_ERROR")
      }
    }

    "respond with FILE_TOO_BIG if huge image passed" in {
      val (scope1, scope2) = TestScope.pair(1, 2)
      catchNewSession(scope1)
      catchNewSession(scope2)

      val respGroup = createGroup(scope1, scope2)

      {
        implicit val scope = scope1

        RequestEditGroupAvatar(respGroup.groupId, respGroup.accessHash, tooLargeFileLocation) :~> <~:(400, "FILE_TOO_BIG")
      }
    }
  }

  import system.dispatcher

  implicit val timeout = 5.seconds

  private var validFileLocation: FileLocation = _
  private var invalidFileLocation: FileLocation = _
  private var tooLargeFileLocation: FileLocation = _

  override def before = {
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

  private def setValidAvatarShouldBeOk(groupId: Int, accessHash: Long)(implicit scope: TestScope) = {
    val (rsp, _) = RequestEditGroupAvatar(groupId, accessHash, validFileLocation) :~> <~:[ResponseAvatarChanged]
    rsp
  }

  private def dbGroup(groupId: Int) =
    GroupRecord.getEntityWithAvatar(groupId).sync().get

  private def dbAvatar(groupId: Int) = dbGroup(groupId)._2.avatar.get
  private def dbFullImage(groupId: Int) = dbAvatar(groupId).fullImage.get
  private def dbLargeImage(groupId: Int) = dbAvatar(groupId).largeImage.get
  private def dbSmallImage(groupId: Int) = dbAvatar(groupId).smallImage.get

  private def dbImageBytes(a: AvatarImage)(implicit scope: TestScope) =
    fr.getFile(a.fileLocation.fileId.toInt).sync()

  private def createGroup(ownerScope: TestScope, scope2: TestScope) = {
    val rqCreateGroup = RequestCreateGroup(
      randomId = 1L,
      title = "Group 3000",
      keyHash = BitVector(1, 1, 1),
      publicKey = BitVector(1, 0, 1, 0),
      broadcast = EncryptedRSABroadcast(
        encryptedMessage = BitVector(1, 2, 3),
        keys = immutable.Seq(
          EncryptedUserAESKeys(
            userId = scope2.user.uid,
            accessHash = ACL.userAccessHash(ownerScope.user.authId, scope2.user),
            keys = immutable.Seq(
              EncryptedAESKey(
                keyHash = scope2.user.publicKeyHash,
                aesEncryptedKey = BitVector(2, 0, 2, 0)
              )
            )
          )
        ),
        ownKeys = immutable.Seq(
          EncryptedAESKey(
            keyHash = ownerScope.user.publicKeyHash,
            aesEncryptedKey = BitVector(2, 0, 2, 0)
          )
        )
      )
    )

    val (resp, _) = {
      implicit val scope = ownerScope
      rqCreateGroup :~> <~:[ResponseCreateGroup]
    }
    Thread.sleep(1000)
    resp
  }
}
