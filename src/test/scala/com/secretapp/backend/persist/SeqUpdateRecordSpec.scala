package com.secretapp.backend.persist

import akka.util.Timeout
import com.datastax.driver.core.ConsistencyLevel
import com.websudos.phantom.Implicits._
import com.secretapp.backend.data.message.{ update => updateProto, _ }
import com.secretapp.backend.data.message.rpc.messaging.EncryptedRSAPackage
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
import com.websudos.util.testing.AsyncAssertionsHelper._

class SeqUpdateRecordSpec extends CassandraSpecification {
  "SeqUpdateRecord" should {
    "get push" in {
      val authId = 123L

      val senderUID = 123
      val destUID = 2
      val pubkeyHash = 1L
      val mid = 1
      val updateMessage = updateProto.Message(
        senderUID, destUID, EncryptedRSAPackage(
          pubkeyHash, BitVector(1, 0, 1, 0), BitVector(1, 2, 3)
        )
      )
      val updateMessageSent = updateProto.MessageSent(destUID, randomId = 5L)

      val efs = for {
        x <- SeqUpdateRecord.push(authId, updateMessage)
        y <- SeqUpdateRecord.push(authId, updateMessageSent)
        f <- SeqUpdateRecord.select.orderBy(_.uuid.asc).where(_.authId eqs authId).fetch
      } yield {
        f
      }

      val mf = efs map {
        case Seq((Entity(key, first), _), _) =>
          first.asInstanceOf[updateProto.Message].message must equalTo(
            EncryptedRSAPackage(
              pubkeyHash, BitVector(1, 0, 1, 0), BitVector(1, 2, 3)
            )
          )
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
      val updateMessage = updateProto.Message(
        senderUID, destUID,
        EncryptedRSAPackage(
          destPublicKeyHash,
          BitVector(1, 0, 1, 0), BitVector(1, 2, 3)
        )
      )

      1 to 1003 foreach { i =>
        Await.result(SeqUpdateRecord.push(authId, updateMessage), Timeout(5000000).duration)
      }

      val fDiffOne = for {
        first <- SeqUpdateRecord.select.where(_.authId eqs authId).orderBy(_.uuid asc).one.map(_.get); firstState = first._1.key
        diff1 <- SeqUpdateRecord.getDifference(authId, Some(firstState), 1); secondState = diff1(0).key
        diff500 <- SeqUpdateRecord.getDifference(authId, Some(secondState), 500)
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
