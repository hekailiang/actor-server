package com.secretapp.backend.helpers

import akka.actor._
import com.datastax.driver.core.{ Session => CSession }
import com.secretapp.backend.data.message.rpc.{ Error, RpcResponse }
import com.secretapp.backend.data.message.struct
import com.secretapp.backend.models
import com.secretapp.backend.persist
import com.secretapp.backend.util.ACL
import scala.collection.immutable
import scala.concurrent.Future

trait PeerHelpers extends UserHelpers {
  val context: ActorContext
  implicit val session: CSession

  import context.{ dispatcher, system }

  protected def withOutPeer(outPeer: struct.OutPeer, currentUser: models.User)(f: => Future[RpcResponse])(implicit session: CSession): Future[RpcResponse] = {
    // TODO: DRY

    outPeer.typ match {
      case struct.PeerType.Private =>
        persist.User.getEntity(outPeer.id) flatMap {
          case Some(userEntity) =>
            if (ACL.userAccessHash(currentUser.authId, userEntity) != outPeer.accessHash) {
              Future.successful(Error(401, "ACCESS_HASH_INVALID", "Invalid access hash.", false))
            } else {
              f
            }
          case None =>
            Future.successful(Error(400, "INTERNAL_ERROR", "Destination user not found", true))
        }
      case struct.PeerType.Group =>
        // TODO: use withGroupOutPeer here
        persist.Group.getEntity(outPeer.id) flatMap {
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

  protected def withGroupOutPeer(groupOutPeer: struct.GroupOutPeer, currentUser: models.User)(f: models.Group => Future[RpcResponse])(implicit session: CSession): Future[RpcResponse] = {
    persist.Group.getEntity(groupOutPeer.id) flatMap {
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

  protected def withOwnGroupOutPeer(groupOutPeer: struct.GroupOutPeer, currentUser: models.User)(f: models.Group => Future[RpcResponse])(implicit session: CSession): Future[RpcResponse] = {
    withGroupOutPeer(groupOutPeer, currentUser) { group =>
      if (group.creatorUserId != currentUser.uid) {
        Future.successful(Error(403, "NO_PERMISSION", "You are not an admin of this group.", true))
      } else {
        f(group)
      }
    }
  }

  protected def withOutPeers(outPeers: immutable.Seq[struct.OutPeer], currentUser: models.User)(f: => Future[RpcResponse])(implicit session: CSession): Future[RpcResponse] = {
    val checkOptsFutures = outPeers map {
      case struct.OutPeer(struct.PeerType.Private, userId, accessHash) =>
        checkUserPeer(userId, accessHash, currentUser)
      case struct.OutPeer(struct.PeerType.Group, groupId, accessHash) =>
        checkGroupPeer(groupId, accessHash)
    }

    renderCheckResult(checkOptsFutures, f)
  }

  protected def withUserOutPeer(userOutPeer: struct.UserOutPeer, currentUser: models.User)(f: => Future[RpcResponse])(implicit session: CSession): Future[RpcResponse] = {
    renderCheckResult(Seq(checkUserPeer(userOutPeer.id, userOutPeer.accessHash, currentUser)), f)
  }

  protected def withUserOutPeers(userOutPeers: immutable.Seq[struct.UserOutPeer], currentUser: models.User)(f: => Future[RpcResponse])(implicit session: CSession): Future[RpcResponse] = {
    val checkOptsFutures = userOutPeers map {
      case struct.UserOutPeer(userId, accessHash) =>
        checkUserPeer(userId, accessHash, currentUser)
    }

    renderCheckResult(checkOptsFutures, f)
  }

  private def checkUserPeer(userId: Int, accessHash: Long, currentUser: models.User): Future[Option[Boolean]] = {
    for {
      userOpt <- persist.User.getEntity(userId)
    } yield {
      userOpt map (ACL.userAccessHash(currentUser.authId, _) == accessHash)
    }
  }

  private def checkGroupPeer(groupId: Int, accessHash: Long): Future[Option[Boolean]] = {
    for {
      groupOpt <- persist.Group.getEntity(groupId)
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
