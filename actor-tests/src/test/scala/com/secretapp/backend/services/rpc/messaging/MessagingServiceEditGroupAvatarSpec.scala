package com.secretapp.backend.services.rpc.user

import com.secretapp.backend.api.AvatarSpecHelpers
import com.secretapp.backend.data.message.struct.{GroupOutPeer, UserOutPeer}
import com.secretapp.backend.models
import com.secretapp.backend.data.message.rpc.update.{ ResponseGetDifference, RequestGetDifference }
import com.secretapp.backend.data.message.rpc.messaging._
import com.secretapp.backend.data.message.update._
import com.secretapp.backend.util.{ACL, AvatarUtils}
import java.nio.file.{ Files, Paths }
import org.specs2.specification.BeforeExample
import scala.collection.immutable
import scala.concurrent.duration._
import scala.util.Random
import scodec.bits._
import com.websudos.util.testing._
import com.secretapp.backend.data.message.rpc.messaging.ResponseEditGroupAvatar
import com.secretapp.backend.persist
import com.secretapp.backend.services.rpc.RpcSpec

class MessagingServiceEditGroupAvatarSpec extends RpcSpec with AvatarSpecHelpers {

  "valid avatar" should {
    "have proper size" in new sqlDb {
      storeImages()

      validOrigAvatarBytes must have size 112527
    }
  }

  "user service on receiving `RequestEditGroupAvatar`" should {
    "respond with `ResponseAvatarChanged`" in new sqlDb {
      storeImages()

      val (scope1, scope2) = TestScope.pair(1, 2)
      catchNewSession(scope1)
      catchNewSession(scope2)

      val respGroup = createGroup(scope1, scope2)

      {
        implicit val scope = scope1

        val r = setValidAvatarShouldBeOk(respGroup.groupPeer.id, respGroup.groupPeer.accessHash)

        r.avatar.fullImage.get.width          should_== validOrigAvatarDimensions._1
        r.avatar.fullImage.get.height         should_== validOrigAvatarDimensions._2
        r.avatar.fullImage.get.fileSize       should_== validOrigAvatarBytes.length
        dbImageBytes(r.avatar.fullImage.get)  should_== validOrigAvatarBytes

        r.avatar.smallImage.get.width         should_== validSmallAvatarDimensions._1
        r.avatar.smallImage.get.height        should_== validSmallAvatarDimensions._2
        r.avatar.smallImage.get.fileSize      should_== validSmallAvatarBytes.length
        dbImageBytes(r.avatar.smallImage.get) should_== validSmallAvatarBytes

        r.avatar.largeImage.get.width         should_== validLargeAvatarDimensions._1
        r.avatar.largeImage.get.height        should_== validLargeAvatarDimensions._2
        r.avatar.largeImage.get.fileSize      should_== validLargeAvatarBytes.length
        dbImageBytes(r.avatar.largeImage.get) should_== validLargeAvatarBytes
      }
    }

    "update group avatar" in new sqlDb {
      storeImages()

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

    "store full image in group avatar" in new sqlDb {
      storeImages()

      val (scope1, scope2) = TestScope.pair(1, 2)
      catchNewSession(scope1)
      catchNewSession(scope2)

      val respGroup = createGroup(scope1, scope2)

      {
        implicit val scope = scope1

        setValidAvatarShouldBeOk(respGroup.groupPeer.id, respGroup.groupPeer.accessHash)

        dbFullImage(respGroup.groupPeer.id).width           should_== validOrigAvatarDimensions._1
        dbFullImage(respGroup.groupPeer.id).height          should_== validOrigAvatarDimensions._2
        dbFullImage(respGroup.groupPeer.id).fileSize        should_== validOrigAvatarBytes.length
        dbImageBytes(dbFullImage(respGroup.groupPeer.id)) should_== validOrigAvatarBytes
      }
    }

    "store large image in group avatar" in new sqlDb {
      storeImages()

      val (scope1, scope2) = TestScope.pair(1, 2)
      catchNewSession(scope1)
      catchNewSession(scope2)

      val respGroup = createGroup(scope1, scope2)

      {
        implicit val scope = scope1

        setValidAvatarShouldBeOk(respGroup.groupPeer.id, respGroup.groupPeer.accessHash)

        dbLargeImage(respGroup.groupPeer.id).width         should_== validLargeAvatarDimensions._1
        dbLargeImage(respGroup.groupPeer.id).height        should_== validLargeAvatarDimensions._2
        dbLargeImage(respGroup.groupPeer.id).fileSize      should_== validLargeAvatarBytes.length
        dbImageBytes(dbLargeImage(respGroup.groupPeer.id)) should_== validLargeAvatarBytes
      }
    }

    "store small image in group avatar" in new sqlDb {
      storeImages()

      val (scope1, scope2) = TestScope.pair(1, 2)
      catchNewSession(scope1)
      catchNewSession(scope2)

      val respGroup = createGroup(scope1, scope2)

      {
        implicit val scope = scope1

        setValidAvatarShouldBeOk(respGroup.groupPeer.id, respGroup.groupPeer.accessHash)

        dbSmallImage(respGroup.groupPeer.id).width         should_== validSmallAvatarDimensions._1
        dbSmallImage(respGroup.groupPeer.id).height        should_== validSmallAvatarDimensions._2
        dbSmallImage(respGroup.groupPeer.id).fileSize      should_== validSmallAvatarBytes.length
        dbImageBytes(dbSmallImage(respGroup.groupPeer.id)) should_== validSmallAvatarBytes
      }
    }

    "append update to chain" in new sqlDb {
      storeImages()

      val (scope1, scope2) = TestScope.pair(1, 2)
      catchNewSession(scope1)
      catchNewSession(scope2)

      val diff1 = {
        implicit val scope = scope1
        RequestGetDifference(0, None) :~> <~:[ResponseGetDifference]
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
        val (diff2, _) = RequestGetDifference(diff1.seq, diff1.state) :~> <~:[ResponseGetDifference]

        diff2.updates.length should beEqualTo(2)
        val update = diff2.updates.last.body.asInstanceOf[SeqUpdateMessage].assertInstanceOf[GroupAvatarChanged]

        val a = update.avatar.get

        a.fullImage.get.width          should_== validOrigAvatarDimensions._1
        a.fullImage.get.height         should_== validOrigAvatarDimensions._2
        a.fullImage.get.fileSize       should_== validOrigAvatarBytes.length
        dbImageBytes(a.fullImage.get)  should_== validOrigAvatarBytes

        a.smallImage.get.width         should_== validSmallAvatarDimensions._1
        a.smallImage.get.height        should_== validSmallAvatarDimensions._2
        a.smallImage.get.fileSize      should_== validSmallAvatarBytes.length
        dbImageBytes(a.smallImage.get) should_== validSmallAvatarBytes

        a.largeImage.get.width         should_== validLargeAvatarDimensions._1
        a.largeImage.get.height        should_== validLargeAvatarDimensions._2
        a.largeImage.get.fileSize      should_== validLargeAvatarBytes.length
        dbImageBytes(a.largeImage.get) should_== validLargeAvatarBytes
      }
    }

    "respond with IMAGE_LOAD_ERROR if invalid image passed" in new sqlDb {
      storeImages()

      val (scope1, scope2) = TestScope.pair(1, 2)
      catchNewSession(scope1)
      catchNewSession(scope2)

      val respGroup = createGroup(scope1, scope2)

      {
        implicit val scope = scope1

        RequestEditGroupAvatar(respGroup.groupPeer, rand.nextLong, invalidFileLocation) :~> <~:(400, "IMAGE_LOAD_ERROR")
      }
    }

    "respond with FILE_TOO_BIG if huge image passed" in new sqlDb {
      storeImages()

      val (scope1, scope2) = TestScope.pair(1, 2)
      catchNewSession(scope1)
      catchNewSession(scope2)

      val respGroup = createGroup(scope1, scope2)

      {
        implicit val scope = scope1

        RequestEditGroupAvatar(respGroup.groupPeer, rand.nextLong, tooLargeFileLocation) :~> <~:(400, "FILE_TOO_BIG")
      }
    }
  }

  "user service on receiving `RequestRemoveGroupAvatar`" should {

    "respond with `ResponseAvatarChanged`" in new sqlDb {
      storeImages()

      val (scope1, scope2) = TestScope.pair(1, 2)
      catchNewSession(scope1)
      catchNewSession(scope2)

      val respGroup = createGroup(scope1, scope2)

      {
        implicit val scope = scope1

        setValidAvatarShouldBeOk(respGroup.groupPeer.id, respGroup.groupPeer.accessHash)
        removeAvatarShouldBeOk(respGroup.groupPeer.id, respGroup.groupPeer.accessHash)
      }
    }

    "update group avatar" in new sqlDb {
      storeImages()

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

    "append update to chain" in new sqlDb {
      storeImages()

      val (scope1, scope2) = TestScope.pair(1, 2)
      catchNewSession(scope1)
      catchNewSession(scope2)

      val diff1 = {
        implicit val scope = scope1
        RequestGetDifference(0, None) :~> <~:[ResponseGetDifference]
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
        val (diff2, _) = RequestGetDifference(diff1.seq, diff1.state) :~> <~:[ResponseGetDifference]
        val update = diff2.updates.last.body.asInstanceOf[SeqUpdateMessage].assertInstanceOf[GroupAvatarChanged]

        update.avatar should beNone
      }
    }
  }

  implicit lazy val ec = system.dispatcher
  implicit val timeout = 5.seconds

  private var validFileLocation: models.FileLocation = _
  private var invalidFileLocation: models.FileLocation = _
  private var tooLargeFileLocation: models.FileLocation = _

  def storeImages() = {
    val fs = storeAvatarFiles(fileAdapter)

    validFileLocation = fs._1
    invalidFileLocation = fs._2
    tooLargeFileLocation = fs._3
  }

  private def setValidAvatarShouldBeOk(groupId: Int, accessHash: Long)(implicit scope: TestScope) = {
    val (rsp, _) = RequestEditGroupAvatar(GroupOutPeer(groupId, accessHash), rand.nextLong, validFileLocation) :~> <~:[ResponseEditGroupAvatar]
    rsp
  }

  private def removeAvatarShouldBeOk(groupId: Int, accessHash: Long)(implicit scope: TestScope) = {
    val (rsp, _) = RequestRemoveGroupAvatar(GroupOutPeer(groupId, accessHash), rand.nextLong) :~> <~:[ResponseSeqDate]
    rsp
  }

  private def dbGroup(groupId: Int) =
    persist.Group.findWithAvatar(groupId).sync().get

  private def dbAvatar(groupId: Int) = dbGroup(groupId)._2.avatar.get
  private def dbFullImage(groupId: Int) = dbAvatar(groupId).fullImage.get
  private def dbLargeImage(groupId: Int) = dbAvatar(groupId).largeImage.get
  private def dbSmallImage(groupId: Int) = dbAvatar(groupId).smallImage.get

  private def dbImageBytes(a: models.AvatarImage)(implicit scope: TestScope) =
    persist.File.readAll(fileAdapter, a.fileLocation.fileId).sync()

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
