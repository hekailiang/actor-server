package com.secretapp.backend.api.history

import com.secretapp.backend.api.{ AvatarSpecHelpers, GroupSpecHelpers, MessagingSpecHelpers }
import com.secretapp.backend.data.message._
import com.secretapp.backend.data.message.struct
import com.secretapp.backend.data.message.rpc._
import com.secretapp.backend.data.message.rpc.messaging._
import com.secretapp.backend.data.message.rpc.history._
import com.secretapp.backend.data.message.rpc.update._
import com.secretapp.backend.data.message.update._
import com.secretapp.backend.models
import com.secretapp.backend.persist
import com.secretapp.backend.services.rpc.RpcSpec
import com.secretapp.backend.util.ACL
import im.actor.testkit.ActorSpecification
import org.joda.time.DateTime
import org.specs2.specification.Step

class HistorySpec extends RpcSpec with MessagingSpecHelpers with GroupSpecHelpers with AvatarSpecHelpers {
  object sqlDb extends sqlDb

  override def is = sequential ^ s2"""
    RequestLoadHistory should
      load history from start if date is 0 ${cases.loadHistory.zeroDate}
    RequestLoadDialogs should
      load dialogs from start if date is 0 ${cases.loadDialogs.zeroDate}
    RequestSendEncryptedMessage should
      create dialog with date 0 ${cases.encrypted.createDialog}
      lift dialog               ${cases.encrypted.liftDialog}
    RequestDeleteMessage handler should
      respond with ResponseVoid   ${cases.deleteMessages.e1}
      delete message from history ${cases.deleteMessages.e2}
    ServiceMessage should be generated at
      RequestCreateGroup       -> GroupCreatedEx                 ${cases.serviceMessages.groupCreated}
      RequestKickUser          -> UserKickedEx                   ${cases.serviceMessages.userKicked}
      RequestInviteUser        -> UserAddedEx                    ${cases.serviceMessages.userAdded}
      RequestChangeGroupTitle  -> GroupChangedTitleEx            ${cases.serviceMessages.changedTitle}
      RequestChangeGroupAvatar -> GroupChangedAvatarEx           ${cases.serviceMessages.changedAvatar}
      RequestRemoveGroupAvatar -> GroupChangedAvatarEx           ${cases.serviceMessages.removedAvatar}
      RequestLeaveGroup        -> UserLeftEx (no dialog reorder) ${cases.serviceMessages.userLeft}
    RequestClearChat should
      clear chat and send updates ${cases.clearChat.e1}
  """

  object cases extends sqlDb {
    def loadHistory(outPeer: struct.OutPeer, date: Long, limit: Int)(implicit scope: TestScope): ResponseLoadHistory = {
      val (rsp, _) = RequestLoadHistory(outPeer, date, limit) :~> <~:[ResponseLoadHistory]
      rsp
    }

    def loadDialogs(date: Long, limit: Int)(implicit scope: TestScope): ResponseLoadDialogs = {
      val (rsp, _) = RequestLoadDialogs(Long.MaxValue, limit) :~> <~:[ResponseLoadDialogs]
      rsp
    }

    object loadHistory {
      val (scope1, scope2) = TestScope.pair(rand.nextInt, rand.nextInt)

      catchNewSession(scope1)

      def zeroDate = {
        using(scope1) { implicit s =>
          sendMessage(scope2.user)
          Thread.sleep(100)

          val respHistory = loadHistory(struct.OutPeer.privat(scope2.user.uid, ACL.userAccessHash(s.user.authId, scope2.user)), 0l, 100)
          respHistory.history.length should_==(1)
        }
      }
    }

    object loadDialogs {
      val (scope1, scope2) = TestScope.pair(rand.nextInt, rand.nextInt)

      catchNewSession(scope1)

      def zeroDate = {
        using(scope1) { implicit s =>
          sendMessage(scope2.user)
          Thread.sleep(100)

          val respDialogs = loadDialogs(0l, 100)
          respDialogs.dialogs.length should_==(1)
        }
      }
    }

    object encrypted {
      val (scope1, scope2) = TestScope.pair(rand.nextInt, rand.nextInt)
      val scope3 = TestScope(rand.nextInt)

      catchNewSession(scope1)
      catchNewSession(scope2)

      def createDialog = {
        using(scope1) { implicit s =>
          sendMessage(scope3.user)
          Thread.sleep(100)

          sendEncryptedMessage(scope2.user)
          Thread.sleep(100)

          val respHistory = loadHistory(struct.OutPeer.privat(scope2.user.uid, ACL.userAccessHash(s.user.authId, scope2.user)), 0l, 100)
          respHistory.history.length should_==(0)

          val respDialogs = loadDialogs(Long.MaxValue, 100)
          val dialogs = respDialogs.dialogs

          dialogs.length should_==(2)
          val dialog = dialogs.head

          dialog.peer should_==(struct.Peer.privat(scope2.user.uid))
          dialog.message should_==(TextMessage(""))
          dialog.randomId should_==(0)
          dialog.date should_==(0)
        }

        using(scope2) { implicit s =>
          val respHistory = loadHistory(struct.OutPeer.privat(scope1.user.uid, ACL.userAccessHash(s.user.authId, scope1.user)), 0l, 100)
          respHistory.history.length should_==(0)

          val respDialogs = loadDialogs(Long.MaxValue, 100)
          val dialogs = respDialogs.dialogs

          dialogs.length should_==(1)
          val dialog = dialogs.head

          dialog.peer should_==(struct.Peer.privat(scope1.user.uid))
          dialog.message should_==(TextMessage(""))
          dialog.randomId should_==(0)
          dialog.date should_==(0)
        }
      }

      def liftDialog = {
        using(scope1) { implicit s =>
          sendMessage(scope2.user, TextMessage("Unencrypted Yolo"))
          Thread.sleep(100)

          sendMessage(scope3.user)
          Thread.sleep(100)

          {
            val dialogs = loadDialogs(Long.MaxValue, 100).dialogs
            dialogs.length should_==(2)
            dialogs.head.peer should_==(struct.Peer.privat(scope3.user.uid))
            dialogs.last.peer should_==(struct.Peer.privat(scope2.user.uid))
            dialogs.last.message should_==(TextMessage("Unencrypted Yolo"))
          }


          sendEncryptedMessage(scope2.user)
          Thread.sleep(100)

          val respDialogs = loadDialogs(Long.MaxValue, 100)
          val dialogs = respDialogs.dialogs

          dialogs.length should_==(2)
          val dialog = dialogs.head

          dialog.peer should_==(struct.Peer.privat(scope2.user.uid))
          dialog.message should_==(TextMessage("Unencrypted Yolo"))
          dialog.randomId should not be_==(0)
          dialog.date should not be be_==(0)
        }
      }
    }

    object deleteMessages {
      val (scope1, scope2) = TestScope.pair(rand.nextInt, rand.nextInt)
      catchNewSession(scope1)

      val outPeer = struct.OutPeer.privat(scope2.user.uid, ACL.userAccessHash(scope1.user.authId, scope2.user))

      def e1 = {
        using(scope1) { implicit scope =>
          RequestSendMessage(
            outPeer = outPeer,
            randomId = 1L,
            message = TextMessage("Yolo1")
          ) :~> <~:[ResponseSeqDate]

          RequestSendMessage(
            outPeer = outPeer,
            randomId = 2L,
            message = TextMessage("Yolo2")
          ) :~> <~:[ResponseSeqDate]

          RequestSendMessage(
            outPeer = outPeer,
            randomId = 3L,
            message = TextMessage("Yolo3")
          ) :~> <~:[ResponseSeqDate]

          RequestDeleteMessage(outPeer, Vector(1L, 3L)) :~> <~:[ResponseVoid]
        }
      }

      def e2 = {
        Thread.sleep(100)

        using(scope1) { implicit scope =>
          val respHistory = loadHistory(outPeer, Long.MaxValue, 3)
          respHistory.history.length should_==(1)
          respHistory.history.head.randomId should_==(2L)
        }
      }
    }

    object serviceMessages {
      val (scope1, scope2) = TestScope.pair(rand.nextInt, rand.nextInt)
      catchNewSession(scope1)
      catchNewSession(scope2)

      val respGroup = createGroup(Vector(scope2.user))(scope1)
      val avatarFileLocation = storeAvatarFiles(fileAdapter)._1

      def groupCreated = {
        val smsg = ServiceMessage("Group created", Some(GroupCreatedExtension()))

        using(scope1) { implicit s =>
          sendMessage(scope2.user)
          Thread.sleep(100)

          val respHistory = loadHistory(respGroup.groupPeer.asOutPeer, Long.MaxValue, 100)
          respHistory.history.length should_==(1)
          respHistory.history.head.message should_==(smsg)

          val respDialogs = loadDialogs(Long.MaxValue, 100)
          val dialogs = respDialogs.dialogs

          dialogs.length should_==(2)
          dialogs.last.peer should_==(struct.Peer.group(respGroup.groupPeer.id))
        }

        using(scope2) { implicit s =>
          val respHistory = loadHistory(respGroup.groupPeer.asOutPeer, Long.MaxValue, 100)
          respHistory.history.length should_==(1)
          respHistory.history.head.message should_==(smsg)

          val respDialogs = loadDialogs(Long.MaxValue, 100)
          val dialogs = respDialogs.dialogs

          dialogs.length should_==(2)
          dialogs.last.peer should_==(struct.Peer.group(respGroup.groupPeer.id))
        }
      }

      def userKicked = {
        val smsg = ServiceMessage("User kicked from the group", Some(UserKickedExtension(scope2.user.uid)))

        using(scope1) { implicit s =>
          kickUser(respGroup.groupPeer, scope2.user)
          Thread.sleep(100)

          val respHistory = loadHistory(respGroup.groupPeer.asOutPeer, Long.MaxValue, 100)
          respHistory.history.length should_==(2)
          val msg = respHistory.history.head
          msg.senderUserId should_==(scope1.user.uid)
          msg.message should_==(smsg)
        }

        using(scope2) { implicit s =>
          val respHistory = loadHistory(respGroup.groupPeer.asOutPeer, Long.MaxValue, 100)
          respHistory.history.length should_==(2)
          val msg = respHistory.history.head
          msg.senderUserId should_==(scope1.user.uid)
          msg.message should_==(smsg)
        }
      }

      def userAdded = {
        val smsg = ServiceMessage("User added to the group", Some(UserAddedExtension(scope2.user.uid)))

        using(scope1) { implicit s =>
          sendMessage(scope2.user)
          Thread.sleep(100)

          inviteUser(respGroup.groupPeer, scope2.user)
          Thread.sleep(100)

          val respHistory = loadHistory(respGroup.groupPeer.asOutPeer, Long.MaxValue, 100)
          respHistory.history.length should_==(3)
          val msg = respHistory.history.head
          msg.senderUserId should_==(scope1.user.uid)
          msg.message should_==(smsg)

          val respDialogs = loadDialogs(Long.MaxValue, 100)
          val dialogs = respDialogs.dialogs

          dialogs.length should_==(2)
          dialogs.head.peer should_==(struct.Peer.group(respGroup.groupPeer.id))
          dialogs.head.message should_==(smsg)
        }

        using(scope2) { implicit s =>
          val respHistory = loadHistory(respGroup.groupPeer.asOutPeer, Long.MaxValue, 100)
          respHistory.history.length should_==(3)
          val msg = respHistory.history.head
          msg.senderUserId should_==(scope1.user.uid)
          msg.message should_==(smsg)

          val respDialogs = loadDialogs(Long.MaxValue, 100)
          val dialogs = respDialogs.dialogs

          dialogs.length should_==(2)
          dialogs.head.peer should_==(struct.Peer.group(respGroup.groupPeer.id))
          dialogs.head.message should_==(smsg)
        }
      }

      def changedTitle = {
        using(scope1) { implicit s =>
          sendMessage(scope2.user)
          Thread.sleep(100)

          editGroupTitle(respGroup.groupPeer, "New title")
          Thread.sleep(100)

          val smsg = ServiceMessage("Group title changed", Some(GroupChangedTitleExtension("New title")))

          val respHistory = loadHistory(respGroup.groupPeer.asOutPeer, Long.MaxValue, 100)
          respHistory.history.length should_==(4)
          val msg = respHistory.history.head
          msg.senderUserId should_==(scope1.user.uid)
          msg.message should_==(smsg)

          val respDialogs = loadDialogs(Long.MaxValue, 100)
          val dialogs = respDialogs.dialogs

          dialogs.length should_==(2)
          dialogs.head.peer should_==(struct.Peer.group(respGroup.groupPeer.id))
          dialogs.head.message should_==(smsg)
        }
      }

      def changedAvatar = {
        using(scope1) { implicit s =>
          sendMessage(scope2.user)
          Thread.sleep(100)

          val respAvatar = editGroupAvatar(respGroup.groupPeer, avatarFileLocation)
          Thread.sleep(100)

          val smsg = ServiceMessage("Group avatar changed", Some(GroupChangedAvatarExtension(Some(respAvatar.avatar))))

          val respHistory = loadHistory(respGroup.groupPeer.asOutPeer, Long.MaxValue, 100)
          respHistory.history.length should_==(5)
          val msg = respHistory.history.head
          msg.senderUserId should_==(scope1.user.uid)
          msg.message should_==(smsg)

          val respDialogs = loadDialogs(Long.MaxValue, 100)
          val dialogs = respDialogs.dialogs

          dialogs.length should_==(2)
          dialogs.head.peer should_==(struct.Peer.group(respGroup.groupPeer.id))
          dialogs.head.message should_==(smsg)
        }
      }

      def removedAvatar = {
        using(scope1) { implicit s =>
          sendMessage(scope2.user)
          Thread.sleep(100)

          removeGroupAvatar(respGroup.groupPeer)
          Thread.sleep(100)

          val smsg = ServiceMessage("Group avatar changed", Some(GroupChangedAvatarExtension(None)))

          val respHistory = loadHistory(respGroup.groupPeer.asOutPeer, Long.MaxValue, 100)
          respHistory.history.length should_==(6)
          val msg = respHistory.history.head
          msg.senderUserId should_==(scope1.user.uid)
          msg.message should_==(smsg)

          val respDialogs = loadDialogs(Long.MaxValue, 100)
          val dialogs = respDialogs.dialogs

          dialogs.length should_==(2)
          dialogs.head.peer should_==(struct.Peer.group(respGroup.groupPeer.id))
          dialogs.head.message should_==(smsg)
        }
      }

      def userLeft = {
        val smsg = ServiceMessage("User left the group", Some(UserLeftExtension()))

        using(scope2) { implicit s =>
          sendMessage(scope1.user)
          Thread.sleep(100)

          leaveGroup(respGroup.groupPeer)
          Thread.sleep(100)

          val respHistory = loadHistory(respGroup.groupPeer.asOutPeer, Long.MaxValue, 100)
          respHistory.history.length should_==(7)
          val msg = respHistory.history.head
          msg.senderUserId should_==(scope2.user.uid)
          msg.message should_==(smsg)

          val respDialogs = loadDialogs(Long.MaxValue, 100)
          val dialogs = respDialogs.dialogs

          dialogs.length should_==(2)
          dialogs.head.peer should_==(struct.Peer.privat(scope1.user.uid))

          dialogs.last.peer should_==(struct.Peer.group(respGroup.groupPeer.id))
          dialogs.last.message should_==(smsg)
        }

        using(scope1) { implicit s =>
          val respHistory = loadHistory(respGroup.groupPeer.asOutPeer, Long.MaxValue, 100)
          respHistory.history.length should_==(7)
          val msg = respHistory.history.head
          msg.senderUserId should_==(scope2.user.uid)
          msg.message should_==(smsg)

          val respDialogs = loadDialogs(Long.MaxValue, 100)
          val dialogs = respDialogs.dialogs

          dialogs.length should_==(2)
          dialogs.head.peer should_==(struct.Peer.privat(scope2.user.uid))

          dialogs.last.peer should_==(struct.Peer.group(respGroup.groupPeer.id))
          dialogs.last.message should_==(smsg)
        }
      }
    }

    object clearChat {
      val (scope1, scope2) = TestScope.pair(rand.nextInt, rand.nextInt)
      catchNewSession(scope1)

      def e1 = using(scope1) { implicit scope =>
        val outPeer = struct.OutPeer.privat(scope2.user.uid, ACL.userAccessHash(scope.user.authId, scope2.user))

        sendMessage(scope2.user, TextMessage("Yolo!"))
        Thread.sleep(100)

        RequestClearChat(outPeer) :~> <~:[ResponseSeq]
        Thread.sleep(100)

        val (diff, _) = RequestGetDifference(0, None) :~> <~:[ResponseGetDifference]

        diff.updates.length should beEqualTo(2)
        val upd = diff.updates.last.body.assertInstanceOf[ChatClear]

        val respHistory = loadHistory(struct.OutPeer.privat(scope2.user.uid, ACL.userAccessHash(scope.user.authId, scope2.user)), 0l, 100)
        respHistory.history.length should_==(0)

        val respDialogs = loadDialogs(Long.MaxValue, 100)
        val dialogs = respDialogs.dialogs

        dialogs.length should_==(1)
        val dialog = dialogs.head
        dialog.message should_==(TextMessage(""))
      }
    }
  }
}
