package com.secretapp.backend.persist

import akka.util.Timeout
import com.newzly.phantom.Implicits._
import com.secretapp.backend.data.message.{update => updateProto, _}
import com.secretapp.backend.protocol.codecs.common.StringCodec
import org.specs2.matcher.NoConcurrentExecutionContext
import org.specs2.mutable._
import scala.collection.immutable.Seq
import scala.concurrent.{Await, ExecutionContext}
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.concurrent.duration._
import scalaz._
import scalaz.Scalaz._
import scodec.bits._

class CommonUpdateRecordSpec extends CassandraSpecification {
  "CommonUpdateRecord" should {
    "get push" in {
      val pubkeyHash = 1L
      val updateMessage = updateProto.Message(pubkeyHash, 1L, 1, 10L, false, None, StringCodec.encode("my message here").toOption.get)
      val ef = for {
        _ <- CommonUpdateRecord.push(pubkeyHash, 1l, updateMessage)
        e <- CommonUpdateRecord.select.where(_.pubkeyHash eqs pubkeyHash).one.map(_.get)
      } yield e

      val mf = ef map {
        case Entity(key, first) =>
          key._1 must equalTo(pubkeyHash)
          first.update.asInstanceOf[updateProto.Message].message must equalTo(StringCodec.encode("my message here").toOption.get)
      }

      mf.await
    }

    "get difference" in {
      val pubkeyHash = 2L
      Await.result(CommonUpdateRecord.truncateTable(session), Timeout(5000000).duration)

      val initialSeq = 0L
      val updateMessage = updateProto.Message(pubkeyHash, initialSeq, 1, 10L, false, None, StringCodec.encode("my message here").toOption.get)

      1 to 1003 foreach { i =>
        Await.result(CommonUpdateRecord.push(pubkeyHash, i, updateMessage), Timeout(5000000).duration)
      }

      val fDiffOne = for {
        first <- CommonUpdateRecord.select.one.map(_.get); firstState = first.key._2
        diff1 <- CommonUpdateRecord.getDifference(pubkeyHash, firstState, 1); secondState = diff1(0).key._2
        diff500 <- CommonUpdateRecord.getDifference(pubkeyHash, secondState, 500)
      } yield {
        val firstts = firstState.timestamp()
        diff1.length must equalTo(1)
        diff1(0).value.seq must equalTo(2)

        diff500.length must equalTo(500)
        diff500(0).value.seq must equalTo(3)
        diff500(100).value.seq must equalTo(103)
        diff500(499).value.seq must equalTo(502)
      }

      fDiffOne.await
    }
  }
}
