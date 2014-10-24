package im.actor.export2js

import im.actor.export2js.macros.JsonType._
import im.actor.export2js.macros.SealedMacros._
import com.secretapp.backend.data.message._
import java.io.FileOutputStream

object Main {
  def main(args: Array[String]) = args.toList match {
    case (path: String) :: Nil =>
      val sealedKlasses = Seq(
        getSealedClass[TransportMessage],
        getSealedClass[rpc.RpcRequestMessage],
        getSealedClass[rpc.RpcResponseMessage],
        getSealedClass[update.UpdateMessage],
        getSealedClass[update.SeqUpdateMessage],
        getSealedClass[update.WeakUpdateMessage],
        getSealedClass[ProtobufMessage]
      )
      println(s"sealedKlasses: $sealedKlasses")
      val coffeeResult = CoffeeScriptBackend(sealedKlasses)
      println(s"coffeeResult: $coffeeResult")
      val file = new FileOutputStream(path, false)
      file.write(coffeeResult.getBytes)
      file.close()
      println(s"Ok.")
    case _ =>
      throw new IllegalArgumentException("You should pass an argument as file path to save")
  }
}
