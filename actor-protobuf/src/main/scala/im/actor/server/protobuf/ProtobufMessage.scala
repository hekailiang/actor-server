package im.actor.server.protobuf

import java.io.Serializable
import scala.reflect.{ ClassTag, classTag }

trait ProtobufMessageLite extends Serializable {
  def toByteArray: Array[Byte]
}

trait ProtobufMessage[M <: com.google.protobuf.GeneratedMessage] extends ProtobufMessageLite {
  def asMessage: M

  def toByteArray: Array[Byte] = asMessage.toByteArray()
}

abstract class ProtobufMessageObject[M <: com.google.protobuf.GeneratedMessage : ClassTag, A <: ProtobufMessage[M]] {
  val parseMessageFrom: (Array[Byte]) => M

  def fromMessage(m: M): A

  def parseFrom(data: Array[Byte]): A =
    parseMessageFrom(data) match {
      case m if classTag[M].runtimeClass.isInstance(m) =>
        fromMessage(m)
      case _ =>
        throw new RuntimeException(s"Not an instance of ${classTag[M]}")
    }
}
