package com.secretapp.backend.services.rpc.messaging

import akka.actor._
import akka.pattern.ask
import com.datastax.driver.core.{ Session => CSession }
import com.secretapp.backend.api.{ SocialProtocol, UpdatesBroker }
import com.secretapp.backend.data.message.struct
import com.secretapp.backend.data.message.rpc.messaging._
import com.secretapp.backend.data.message.rpc.{ Error, Ok, RpcResponse, ResponseAvatarChanged, ResponseVoid }
import com.secretapp.backend.data.message.rpc.update._
import com.secretapp.backend.data.message.{ update => updateProto }
import com.secretapp.backend.models
import com.secretapp.backend.helpers._
import com.secretapp.backend.persist
import com.secretapp.backend.services.common.RandomService
import com.secretapp.backend.util.{ACL, AvatarUtils}
import java.util.UUID
import scala.collection.immutable
import scala.concurrent.Future
import scala.language.postfixOps
import scalaz.Scalaz._
import scalaz._
import scodec.bits._

trait GroupHandlers extends RandomService with UserHelpers with GroupHelpers with PeerHelpers with UpdatesHelpers {
  self: Handler =>

  import context.{ dispatcher, system }
  import UpdatesBroker._

  implicit val session: CSession

  val handleGroup: RequestMatcher = {
    case RequestCreateGroup(randomId, title, users) =>
      handleRequestCreateGroup(randomId, title, users)
  }

  protected def handleRequestCreateGroup(
    randomId: Long,
    title: String,
    users: immutable.Seq[struct.UserOutPeer]
  ): Future[RpcResponse] = {
    val id = rand.nextInt

    val group = models.Group(id, currentUser.uid, rand.nextLong(), title)

    withUserOutPeers(users, currentUser) {
      val createGroupModelF = persist.Group.insertEntity(group)

      val userIds = (users map (_.id) toSet) + currentUser.uid

      val addUsersF = userIds map { userId =>
        // TODO: use shapeless-contrib here after upgrading to scala 2.11
        Future.sequence(Seq(
          persist.GroupUser.addUser(group.id, userId),
          persist.UserGroup.addGroup(userId, group.id)
        ))
      }

      // use shapeless, shapeless everywhere!
      val groupCreatedF = for {
        _ <- createGroupModelF
        _ <- Future.sequence(addUsersF)
      } yield {}

      groupCreatedF flatMap { _ =>
        val date = System.currentTimeMillis()

        userIds foreach { userId =>
          for {
            authIds <- getAuthIds(userId)
          } yield {
            authIds foreach { authId =>
              if (authId != currentUser.authId) {
                writeNewUpdate(authId, updateProto.GroupInvite(
                  groupId = group.id,
                  inviterUserId = currentUser.uid,
                  date = date
                ))
              }
            }
          }
        }

        withNewUpdateState(
          currentUser.authId,
          updateProto.GroupInvite(
            groupId = group.id,
            inviterUserId = currentUser.uid,
            date = date
          )
        ) { s =>
          val res = ResponseSeq(s._1, Some(s._2))
          Ok(res)
        }
      }
    }
  }

  protected def handleRequestEditGroupTitle(
    groupPeer: struct.GroupOutPeer,
    title: String
  ): Future[RpcResponse] = ???

  protected def handleRequestEditGroupAvatar(
    groupPeer: struct.GroupOutPeer,
    fileLocation: models.FileLocation
  ): Future[RpcResponse] = ???

  protected def handleRequestRemoveGroupAvatar(
    groupPeer: struct.GroupOutPeer
  ): Future[RpcResponse] = ???

  protected def handleRequestInviteUsers(
    groupPeer: struct.GroupOutPeer,
    users: immutable.Seq[struct.UserOutPeer]
  ): Future[RpcResponse] = ???

  protected def handleRequestLeaveGroup(
    groupPeer: struct.GroupOutPeer
  ): Future[RpcResponse] = ???

  protected def handleRequestDeleteGroup(
    groupPeer: struct.GroupOutPeer
  ): Future[RpcResponse] = ???

  protected def handleRequestRemoveUsers(
    groupPeer: struct.GroupOutPeer,
    users: immutable.Seq[struct.UserOutPeer]
  ): Future[RpcResponse] = ???
}
