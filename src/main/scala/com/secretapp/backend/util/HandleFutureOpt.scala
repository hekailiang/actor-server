package com.secretapp.backend.util

import scala.concurrent.{ExecutionContext, Future}
import scalaz._
import Scalaz._

object HandleFutureOpt {
  type HandleResult[E, A] = Future[E \/ A]
  type HandleFutureOpt[A] = Future[Option[A]]

  final case class Handle[+A, E](optF: HandleFutureOpt[A], e: E) {
    def flatMap[U](f: A => HandleResult[E, U])(implicit ec: ExecutionContext): HandleResult[E, U] = {
      optF flatMap {
        case Some(res) => f(res)
        case None => Future.successful(e.left)
      }
    }

    def map[U](f: A => E \/ U)(implicit ec: ExecutionContext): HandleResult[E, U] = {
      optF map {
        case Some(res) => f(res)
        case None => e.left
      }
    }
  }

  def futOptHandle[A, E](f: HandleFutureOpt[A], e: E): Handle[A, E] = {
    Handle[A, E](f, e)
  }
}
