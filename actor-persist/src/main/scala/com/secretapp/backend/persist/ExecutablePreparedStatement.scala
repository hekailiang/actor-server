package com.secretapp.backend.persist

import com.datastax.driver.core.BoundStatement
import com.datastax.driver.core.{ ResultSet, Session, Statement }
import com.google.common.util.concurrent.{ FutureCallback, Futures }
import java.util.concurrent.Executor
import scala.concurrent.Future
import scala.concurrent.{ ExecutionContext, Future => ScalaFuture, Promise => ScalaPromise }

abstract class ExecutablePreparedStatement(implicit val session: Session, context: ExecutionContext with Executor) {
  val query: String

  private lazy val statement = session.prepare(
    query)

  def execute(values: java.lang.Object*): Future[ResultSet] = {
    val bs = new BoundStatement(statement).bind(values: _*)
    statementToFuture(bs)
  }

  private def statementToFuture(s: Statement)(implicit session: Session): ScalaFuture[ResultSet] = {
    val promise = ScalaPromise[ResultSet]()

    val future = session.executeAsync(s)

    val callback = new FutureCallback[ResultSet] {
      def onSuccess(result: ResultSet): Unit = {
        promise success result
      }

      def onFailure(err: Throwable): Unit = {
        promise failure err
      }
    }

    Futures.addCallback(future, callback, context)

    promise.future
  }
}
