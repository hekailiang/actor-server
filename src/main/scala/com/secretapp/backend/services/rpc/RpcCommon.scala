package com.secretapp.backend.services.rpc

import akka.actor.Actor
import com.secretapp.backend.data.message._
import com.secretapp.backend.data.message.rpc._
import com.secretapp.backend.data.transport._
import com.secretapp.backend.services.common.PackageCommon
import scala.concurrent.{ExecutionContext, Future}
import scalaz._
import Scalaz._
import scala.util.{ Success, Failure }
import scala.language.implicitConversions

trait RpcCommon { self: Actor with PackageCommon =>

  // TODO: Abstract over left, it is sufficient for left to be `PlusEmpty`.
  case class Result[A](hr: Future[Error \/ A])

  object Result {
    implicit def toResult[A](hr: Future[Error \/ A]): Result[A] = Result(hr)

    implicit def fromResult[A](r: Result[A]): Future[Error \/ A] = r.hr

    implicit def monad(implicit ec: ExecutionContext): MonadPlus[Result] = new MonadPlus[Result] {
      override def bind[A, B](fa: Result[A])(f: (A) => Result[B]): Result[B] =
        fa.hr flatMap { va =>
          va map { ra =>
            f(ra).hr
          } valueOr { la =>
            Future successful la.left
          }
        }

      override def point[A](a: => A): Result[A] = Future successful a.right

      override def empty[A]: Result[A] = Future successful internalError.left

      override def plus[A](a: Result[A], b: => Result[A]): Result[A] = a.hr flatMap {
        _.fold(_ => b.hr, a => Future successful a.right)
      }
    }
  }

  type RpcResult = Result[RpcResponseMessage]

  // TODO: Should we accumuate all errors in one place?
  val internalError = Error(400, "INTERNAL_ERROR", "", canTryAgain = true)

  def sendRpcResult(p: Package, messageId: Long)(res: RpcResult)(implicit ec: ExecutionContext): Unit = {
    res onComplete {
      case Success(res) =>
        val message = res.fold(identity, Ok(_))
        sendReply(p.replyWith(messageId, RpcResponseBox(messageId, message)).right)
      case Failure(e) =>
        sendReply(p.replyWith(messageId, RpcResponseBox(messageId, internalError)).right)
        throw e
    }
  }
}
