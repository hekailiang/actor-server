package com.secretapp.backend.persist

import akka.util.Timeout
import com.datastax.driver.core.ConsistencyLevel
import com.websudos.phantom.Implicits._
import com.secretapp.backend.data.message.{ update => updateProto, _ }
import com.secretapp.backend.protocol.codecs.common.StringCodec
import scala.concurrent.{ Await }
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.concurrent.duration._
import scala.util.Failure
import scala.util.Success
import scalaz._
import scalaz.Scalaz._
import scodec.bits._
import com.newzly.util.testing.AsyncAssertionsHelper._

class CommonUpdateRecordSpec extends CassandraSpecification {
  "CommonUpdateRecord" should {
    "get push" in {
      val authId = 123L

      val senderUID = 123
      val destUID = 2
      val pubkeyHash = 1L
      val mid = 1
      val updateMessage = updateProto.Message(senderUID, destUID, mid, pubkeyHash, false, None, StringCodec.encode("my message here").toOption.get)
      val updateMessageSent = updateProto.MessageSent(mid + 1, randomId = 5L)

      val efs = for {
        x <- CommonUpdateRecord.push(authId, updateMessage)
        y <- CommonUpdateRecord.push(authId, updateMessageSent)
        f <- CommonUpdateRecord.select.orderBy(_.uuid.asc).where(_.authId eqs authId).fetch
      } yield {
        f
      }

      val mf = efs map {
        case Seq(Entity(key, first), _) =>
          first.asInstanceOf[updateProto.Message].message must equalTo(StringCodec.encode("my message here").toOption.get)
        case x => throw new Exception(s"Unexpected updates count $x")
      }

      mf.await(timeout = DurationInt(10).seconds)
    }

    "get difference" in {
      val authId = 3L
      val publicKeyHash = 2L

      val senderUID = 3
      val destUID = 2
      val destPublicKeyHash = 3L
      val mid = 1

      val initialSeq = 0L
      val updateMessage = updateProto.Message(senderUID, destUID, mid, destPublicKeyHash, false, None, StringCodec.encode("my message here").toOption.get)

      1 to 1003 foreach { i =>
        Await.result(CommonUpdateRecord.push(authId, updateMessage), Timeout(5000000).duration)
      }

      val fDiffOne = for {
        first <- CommonUpdateRecord.select.where(_.authId eqs authId).orderBy(_.uuid asc).one.map(_.get); firstState = first.key
        diff1 <- CommonUpdateRecord.getDifference(authId, Some(firstState), 1); secondState = diff1(0).key
        diff500 <- CommonUpdateRecord.getDifference(authId, Some(secondState), 500)
      } yield {
        val firstts = firstState.timestamp()
        diff1.length must equalTo(1)
        //diff1(0).value.seq must equalTo(2)

        diff500.length must equalTo(500)
        //diff500(0).value.seq must equalTo(3)
        //diff500(100).value.seq must equalTo(103)
        //diff500(499).value.seq must equalTo(502)*/
      }

      fDiffOne.await
    }
  }
}
