package im.actor.util.logging

import akka.actor.Actor
import akka.event.{ DiagnosticLoggingAdapter, Logging }

trait MDCActorLogging {
  this: Actor =>

  val log: DiagnosticLoggingAdapter = Logging(this)

  protected def mdc = {
    Map.empty[String, Any]
  }

  protected def withMDC[A](_mdc: Map[String, Any])(f: => A): A = {
    log.mdc(mdc ++ _mdc)
    val res = f
    log.clearMDC()
    res
  }

  protected def withMDC[A](f: => A): A = withMDC(Map.empty[String, Any])(f)
}
