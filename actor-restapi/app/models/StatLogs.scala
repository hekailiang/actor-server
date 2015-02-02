package models

import com.secretapp.backend.persist.events.LogEvent
import scala.concurrent.ExecutionContext

object StatLogs {
  def stats()(implicit ec: ExecutionContext) = {
    for { entries <- LogEvent.stats() }
    yield entries
  }
}
