package com.secretapp.backend.helpers

import akka.actor._
import com.secretapp.backend.data.message.rpc.{ Error, RpcResponse }
import com.secretapp.backend.data.message.struct
import com.secretapp.backend.models
import com.secretapp.backend.persist
import com.secretapp.backend.persist.GroupUserMeta
import com.secretapp.backend.util.ACL
import scala.collection.immutable
import scala.concurrent.Future

trait PeerHelpers extends UserHelpers {
  val context: ActorContext

  import context.{ dispatcher, system }

  protected def withOutPeer(outPeer: struct.OutPeer, currentUser: models.User)(f: => Future[RpcResponse]): Future[RpcResponse] = {
    // TODO: DRY

    outPeer.typ match {
      case models.PeerType.Private =>
        persist.User.find(outPeer.id)(authId = None) flatMap {
          case Some(userEntity) =>
            if (ACL.userAccessHash(currentUser.authId, userEntity) != outPeer.accessHash) {
              Future.successful(Error(401, "ACCESS_HASH_INVALID", "Invalid access hash.", false))
            } else {
              f
            }
          case None =>
            Future.successful(Error(400, "INTERNAL_ERROR", "Destination user not found", true))
        }
      case models.PeerType.Group =>
        // TODO: use withGroupOutPeer here
        persist.Group.find(outPeer.id) flatMap {
          case Some(groupEntity) =>
            if (groupEntity.accessHash != outPeer.accessHash) {
              Future.successful(Error(401, "ACCESS_HASH_INVALID", "Invalid access hash.", false))
            } else {
              f
            }
          case None =>
            Future.successful(Error(400, "INTERNAL_ERROR", "Destination group not found", true))
        }
    }
  }

  protected def withGroupOutPeer(groupOutPeer: struct.GroupOutPeer, currentUser: models.User)(f: models.Group => Future[RpcResponse]): Future[RpcResponse] = {
    persist.Group.find(groupOutPeer.id) flatMap {
      case Some(group) =>
        if (group.accessHash != groupOutPeer.accessHash) {
          Future.successful(Error(401, "ACCESS_HASH_INVALID", "Invalid access hash.", false))
        } else {
          f(group)
        }
      case None =>
        Future.successful(Error(400, "INTERNAL_ERROR", "Destination group not found", true))
    }
  }

  protected def withKickableGroupMember(groupOutPeer: struct.GroupOutPeer, currentUser: models.User, kickUserOutPeer: struct.UserOutPeer)(f: models.Group => Future[RpcResponse]): Future[RpcResponse] = {
    withGroupOutPeer(groupOutPeer, currentUser) { group =>
      if (group.creatorUserId != currentUser.uid) {
        persist.GroupUser.findGroupUser(group.id, kickUserOutPeer.id).flatMap {
          case Some(GroupUserMeta(inviterUserId, _)) =>
            if (inviterUserId == currentUser.uid) {
              f(group)
            } else {
              Future.successful(Error(403, "NO_PERMISSION", "You are permitted to kick this user.", false))
            }
          case None => Future.successful(Error(404, "USER_NOT_FOUND", "User is not a group member.", false))
        }
      } else {
        f(group)
      }
    }
  }

  protected def withOwnGroupOutPeer(groupOutPeer: struct.GroupOutPeer, currentUser: models.User)(f: models.Group => Future[RpcResponse]): Future[RpcResponse] = {
    withGroupOutPeer(groupOutPeer, currentUser) { group =>
      if (group.creatorUserId != currentUser.uid) {
        Future.successful(Error(403, "NO_PERMISSION", "You are not an admin of this group.", false))
      } else {
        f(group)
      }
    }
  }

  protected def withOutPeers(outPeers: immutable.Seq[struct.OutPeer], currentUser: models.User)(f: => Future[RpcResponse]): Future[RpcResponse] = {
    val checkOptsFutures = outPeers map {
      case struct.OutPeer(models.PeerType.Private, userId, accessHash) =>
        checkUserPeer(userId, accessHash, currentUser)
      case struct.OutPeer(models.PeerType.Group, groupId, accessHash) =>
        checkGroupPeer(groupId, accessHash)
    }

    renderCheckResult(checkOptsFutures, f)
  }

  protected def withUserOutPeer(userOutPeer: struct.UserOutPeer, currentUser: models.User)(f: => Future[RpcResponse]): Future[RpcResponse] = {
    renderCheckResult(Seq(checkUserPeer(userOutPeer.id, userOutPeer.accessHash, currentUser)), f)
  }

  protected def withUserOutPeers(userOutPeers: immutable.Seq[struct.UserOutPeer], currentUser: models.User)(f: => Future[RpcResponse]): Future[RpcResponse] = {
    val checkOptsFutures = userOutPeers map {
      case struct.UserOutPeer(userId, accessHash) =>
        checkUserPeer(userId, accessHash, currentUser)
    }

    renderCheckResult(checkOptsFutures, f)
  }

  private def checkUserPeer(userId: Int, accessHash: Long, currentUser: models.User): Future[Option[Boolean]] = {
    for {
      userOpt <- persist.User.find(userId)(authId = None)
    } yield {
      userOpt map (ACL.userAccessHash(currentUser.authId, _) == accessHash)
    }
  }

  private def checkGroupPeer(groupId: Int, accessHash: Long): Future[Option[Boolean]] = {
    for {
      groupOpt <- persist.Group.find(groupId)
    } yield {
      groupOpt map (_.accessHash == accessHash)
    }
  }

  private def renderCheckResult(checkOptsFutures: Seq[Future[Option[Boolean]]], f: => Future[RpcResponse]): Future[RpcResponse] = {
    Future.sequence(checkOptsFutures) flatMap { checkOpts =>
      if (checkOpts.contains(None)) {
        Future.successful(Error(404, "PEER_NOT_FOUND", "Peer not found", false))
      } else if (checkOpts.flatten.contains(false)) {
        Future.successful(Error(401, "ACCESS_HASH_INVALID", "Invalid access hash.", false))
      } else {
        f
      }
    }
  }
}
