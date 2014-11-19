package com.secretapp.backend.services.rpc.user

import java.nio.file.{ Files, Paths }
import com.secretapp.backend.data.message.struct.{GroupOutPeer, UserOutPeer}
import com.secretapp.backend.models
import com.secretapp.backend.data.message.rpc.update.{ Difference, RequestGetDifference }
import com.secretapp.backend.data.message.rpc.messaging._
import com.secretapp.backend.data.message.update._
import com.secretapp.backend.util.{ACL, AvatarUtils}
import org.specs2.specification.BeforeExample
import scala.collection.immutable
import scala.util.Random
import scodec.bits._
import com.websudos.util.testing._
import com.secretapp.backend.data.message.rpc.ResponseAvatarChanged
import com.secretapp.backend.persist
import com.secretapp.backend.services.rpc.RpcSpec

class MessagingServiceEditGroupAvatarSpec extends RpcSpec with BeforeExample {

  "valid avatar" should {
    "have proper size" in {
      validOrigBytes must have size 112527
    }
  }

  "user service on receiving `RequestEditGroupAvatar`" should {

    "respond with `ResponseAvatarChanged`" in {
      val (scope1, scope2) = TestScope.pair(1, 2)
      catchNewSession(scope1)
      catchNewSession(scope2)

      val respGroup = createGroup(scope1, scope2)

      {
        implicit val scope = scope1

        val r = setValidAvatarShouldBeOk(respGroup.groupPeer.id, respGroup.groupPeer.accessHash)

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

    "update group avatar" in {
      val (scope1, scope2) = TestScope.pair(1, 2)
      catchNewSession(scope1)
      catchNewSession(scope2)

      val respGroup = createGroup(scope1, scope2)

      val r = {
        implicit val scope = scope1

        setValidAvatarShouldBeOk(respGroup.groupPeer.id, respGroup.groupPeer.accessHash)
      }

      dbGroup(respGroup.groupPeer.id)._2.avatar    should beSome
      dbAvatar(respGroup.groupPeer.id).fullImage   should beSome
      dbAvatar(respGroup.groupPeer.id).smallImage  should beSome
      dbAvatar(respGroup.groupPeer.id).largeImage  should beSome
    }

    "store full image in group avatar" in {
      val (scope1, scope2) = TestScope.pair(1, 2)
      catchNewSession(scope1)
      catchNewSession(scope2)

      val respGroup = createGroup(scope1, scope2)

      {
        implicit val scope = scope1

        setValidAvatarShouldBeOk(respGroup.groupPeer.id, respGroup.groupPeer.accessHash)

        dbFullImage(respGroup.groupPeer.id).width           should_== validOrigDimensions._1
        dbFullImage(respGroup.groupPeer.id).height          should_== validOrigDimensions._2
        dbFullImage(respGroup.groupPeer.id).fileSize        should_== validOrigBytes.length
        dbImageBytes(dbFullImage(respGroup.groupPeer.id)) should_== validOrigBytes
      }
    }

    "store large image in group avatar" in {
      val (scope1, scope2) = TestScope.pair(1, 2)
      catchNewSession(scope1)
      catchNewSession(scope2)

      val respGroup = createGroup(scope1, scope2)

      {
        implicit val scope = scope1

        setValidAvatarShouldBeOk(respGroup.groupPeer.id, respGroup.groupPeer.accessHash)

        dbLargeImage(respGroup.groupPeer.id).width         should_== validLargeDimensions._1
        dbLargeImage(respGroup.groupPeer.id).height        should_== validLargeDimensions._2
        dbLargeImage(respGroup.groupPeer.id).fileSize      should_== validLargeBytes.length
        dbImageBytes(dbLargeImage(respGroup.groupPeer.id)) should_== validLargeBytes
      }
    }

    "store small image in group avatar" in {
      val (scope1, scope2) = TestScope.pair(1, 2)
      catchNewSession(scope1)
      catchNewSession(scope2)

      val respGroup = createGroup(scope1, scope2)

      {
        implicit val scope = scope1

        setValidAvatarShouldBeOk(respGroup.groupPeer.id, respGroup.groupPeer.accessHash)

        dbSmallImage(respGroup.groupPeer.id).width         should_== validSmallDimensions._1
        dbSmallImage(respGroup.groupPeer.id).height        should_== validSmallDimensions._2
        dbSmallImage(respGroup.groupPeer.id).fileSize      should_== validSmallBytes.length
        dbImageBytes(dbSmallImage(respGroup.groupPeer.id)) should_== validSmallBytes
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
        setValidAvatarShouldBeOk(respGroup.groupPeer.id, respGroup.groupPeer.accessHash)
        respGroup
      }

      Thread.sleep(1000)

      {
        implicit val scope = scope1
        val (diff2, updates2) = RequestGetDifference(diff1.seq, diff1.state) :~> <~:[Difference]

        updates2.length should beEqualTo(2)
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

        RequestEditGroupAvatar(respGroup.groupPeer, invalidFileLocation) :~> <~:(400, "IMAGE_LOAD_ERROR")
      }
    }

    "respond with FILE_TOO_BIG if huge image passed" in {
      val (scope1, scope2) = TestScope.pair(1, 2)
      catchNewSession(scope1)
      catchNewSession(scope2)

      val respGroup = createGroup(scope1, scope2)

      {
        implicit val scope = scope1

        RequestEditGroupAvatar(respGroup.groupPeer, tooLargeFileLocation) :~> <~:(400, "FILE_TOO_BIG")
      }
    }
  }

  "user service on receiving `RequestRemoveGroupAvatar`" should {

    "respond with `ResponseAvatarChanged`" in {
      val (scope1, scope2) = TestScope.pair(1, 2)
      catchNewSession(scope1)
      catchNewSession(scope2)

      val respGroup = createGroup(scope1, scope2)

      {
        implicit val scope = scope1

        setValidAvatarShouldBeOk(respGroup.groupPeer.id, respGroup.groupPeer.accessHash)
        val r = removeAvatarShouldBeOk(respGroup.groupPeer.id, respGroup.groupPeer.accessHash)

        r.avatar.fullImage  should beNone
        r.avatar.smallImage should beNone
        r.avatar.largeImage should beNone
      }
    }

    "update group avatar" in {
      val (scope1, scope2) = TestScope.pair(1, 2)
      catchNewSession(scope1)
      catchNewSession(scope2)

      val respGroup = createGroup(scope1, scope2)

      val r = {
        implicit val scope = scope1

        setValidAvatarShouldBeOk(respGroup.groupPeer.id, respGroup.groupPeer.accessHash)
        removeAvatarShouldBeOk(respGroup.groupPeer.id, respGroup.groupPeer.accessHash)
      }

      dbGroup(respGroup.groupPeer.id)._2.avatar should beNone
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
        setValidAvatarShouldBeOk(respGroup.groupPeer.id, respGroup.groupPeer.accessHash)
        removeAvatarShouldBeOk(respGroup.groupPeer.id, respGroup.groupPeer.accessHash)
        respGroup
      }

      Thread.sleep(1000)

      {
        implicit val scope = scope1
        val (diff2, updates2) = RequestGetDifference(diff1.seq, diff1.state) :~> <~:[Difference]
        val update = updates2.last.body.asInstanceOf[SeqUpdate].body.assertInstanceOf[GroupAvatarChanged]

        update.avatar should beNone
      }
    }
  }

  import system.dispatcher

  implicit val timeout = 5.seconds

  private var validFileLocation: models.FileLocation = _
  private var invalidFileLocation: models.FileLocation = _
  private var tooLargeFileLocation: models.FileLocation = _

  override def before = {
    validFileLocation = storeImage(42, validOrigBytes)
    invalidFileLocation = storeImage(43, invalidBytes)
    tooLargeFileLocation = storeImage(44, tooLargeBytes)
  }

  private val fr = new persist.File

  private val validOrigBytes =
    Files.readAllBytes(Paths.get(getClass.getResource("/valid-avatar.jpg").toURI))

  private val invalidBytes = Stream.continually(Random.nextInt().toByte).take(50000).toArray

  private val tooLargeBytes =
    Files.readAllBytes(Paths.get(getClass.getResource("/too-large-avatar.jpg").toURI))

  private val validOrigDimensions = AvatarUtils.dimensions(validOrigBytes).sync()

  private val validLargeBytes = AvatarUtils.resizeToLarge(validOrigBytes).sync()
  private val validLargeDimensions = (200, 200)

  private val validSmallBytes = AvatarUtils.resizeToSmall(validOrigBytes).sync()
  private val validSmallDimensions = (100, 100)

  private def storeImage(fileId: Int, bytes: Array[Byte]): models.FileLocation = {
    val fileSalt = (new Random).nextString(30)

    val ffl = for (
      _    <- fr.createFile(fileId, fileSalt);
      _    <- fr.write(fileId, 0, bytes);
      hash <- ACL.fileAccessHash(fr, fileId);
      fl    = models.FileLocation(fileId, hash)
    ) yield fl

    ffl.sync()
  }

  private def setValidAvatarShouldBeOk(groupId: Int, accessHash: Long)(implicit scope: TestScope) = {
    val (rsp, _) = RequestEditGroupAvatar(GroupOutPeer(groupId, accessHash), validFileLocation) :~> <~:[ResponseAvatarChanged]
    rsp
  }

  private def removeAvatarShouldBeOk(groupId: Int, accessHash: Long)(implicit scope: TestScope) = {
    val (rsp, _) = RequestRemoveGroupAvatar(GroupOutPeer(groupId, accessHash)) :~> <~:[ResponseAvatarChanged]
    rsp
  }

  private def dbGroup(groupId: Int) =
    persist.Group.getEntityWithAvatar(groupId).sync().get

  private def dbAvatar(groupId: Int) = dbGroup(groupId)._2.avatar.get
  private def dbFullImage(groupId: Int) = dbAvatar(groupId).fullImage.get
  private def dbLargeImage(groupId: Int) = dbAvatar(groupId).largeImage.get
  private def dbSmallImage(groupId: Int) = dbAvatar(groupId).smallImage.get

  private def dbImageBytes(a: models.AvatarImage)(implicit scope: TestScope) =
    fr.getFile(a.fileLocation.fileId.toInt).sync()

  private def createGroup(ownerScope: TestScope, scope2: TestScope) = {
    val rqCreateGroup = RequestCreateGroup(
      randomId = 1L,
      title = "Group 3000",
      users = immutable.Seq(
        UserOutPeer(scope2.user.uid, ACL.userAccessHash(ownerScope.user.authId, scope2.user))
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
