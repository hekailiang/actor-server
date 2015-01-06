package im.actor.server.protobuf

import akka.actor.ExtendedActorSystem
import akka.serialization.Serializer

class ProtobufMessageSerializer(val system: ExtendedActorSystem) extends Serializer {
  val ARRAY_OF_BYTE_ARRAY = Array[Class[_]](classOf[Array[Byte]])
  def includeManifest: Boolean = true

  def identifier = -300

  def toBinary(o: AnyRef): Array[Byte] = o match {
    case m: ProtobufMessageLite =>
      m.toByteArray
    case _ => throw new IllegalArgumentException("Can't serialize a non ProtobufMessage message [" + o + "]")
  }

  def fromBinary(bytes: Array[Byte], clazz: Option[Class[_]]): AnyRef = {
    clazz match {
      case None =>
        throw new IllegalArgumentException("Need a message class to deserialize ProtobufMessage")
      case Some(c) =>
        val co = Class.forName(c.getName + "$")
        val obj = co.getField("MODULE$").get(co)
        co.getMethod("parseFrom", ARRAY_OF_BYTE_ARRAY: _*).invoke(obj, bytes)
    }
  }
}
