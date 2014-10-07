package com.secretapp.backend.data.json.message.update

import com.secretapp.backend.data.json._
import com.secretapp.backend.data.json.JsonSpec
import com.secretapp.backend.data.json.JsonSpec._
import com.secretapp.backend.data.message.update._
import com.secretapp.backend.data.json.message.struct.JsonFormatsSpec._
import scalaz._
import Scalaz._

class JsonFormatsSpec extends JsonSpec {

  "UpdateMessage (de)serializer" should {

    "(de)serialize SeqUpdate" in {
      val (state, stateJson) = genBitVector
      val v = SeqUpdate(1, state, NewDevice(2, 3))
      val j = withHeader(SeqUpdate.updateHeader)(
        "seq" -> 1,
        "state" -> stateJson,
        "body" -> withHeader(NewDevice.seqUpdateHeader)(
          "uid" -> 2,
          "keyHash" -> "3"
        )
      )
      testToAndFromJson[UpdateMessage](j, v)
    }

    "(de)serialize SeqUpdateTooLong" in {
      val v = SeqUpdateTooLong()
      val j = withHeader(SeqUpdateTooLong.updateHeader)()
      testToAndFromJson[UpdateMessage](j, v)
    }

    "(de)serialize WeakUpdate" in {
      val v = WeakUpdate(1, UserOnline(2))
      val j = withHeader(WeakUpdate.updateHeader)(
        "date" -> "1",
        "body" -> withHeader(UserOnline.weakUpdateHeader)(
          "uid" -> 2
        )
      )
      testToAndFromJson[UpdateMessage](j, v)
    }

  }

  "SeqUpdateMessage (de)serializer" should {

    "(de)serialize AvatarChanged" in {
      val (avatar, avatarJson) = genAvatar
      val v = AvatarChanged(1, avatar.some)
      val j = withHeader(AvatarChanged.seqUpdateHeader)(
        "uid" -> 1,
        "avatar" -> avatarJson
      )
      testToAndFromJson[SeqUpdateMessage](j, v)
    }

    "(de)serialize ContactRegistered" in {
      val v = ContactRegistered(1)
      val j = withHeader(ContactRegistered.seqUpdateHeader)(
        "userId" -> 1
      )
      testToAndFromJson[SeqUpdateMessage](j, v)
    }

    "(de)serialize Message" in {
      val (aesEncryptedKey, aesEncryptedKeyJson) = genBitVector
      val (message, messageJson) = genBitVector
      val v = Message(1, 2, 3, aesEncryptedKey, message)
      val j = withHeader(Message.seqUpdateHeader)(
        "senderUID" -> 1,
        "destUID" -> 2,
        "keyHash" -> "3",
        "aesEncryptedKey" -> aesEncryptedKeyJson,
        "message" -> messageJson
      )
      testToAndFromJson[SeqUpdateMessage](j, v)
    }

    "(de)serialize MessageRead" in {
      val v = MessageRead(1, 2)
      val j = withHeader(MessageRead.seqUpdateHeader)(
        "uid" -> 1,
        "randomId" -> "2"
      )
      testToAndFromJson[SeqUpdateMessage](j, v)
    }

    "(de)serialize MessageReceived" in {
      val v = MessageReceived(1, 2)
      val j = withHeader(MessageReceived.seqUpdateHeader)(
        "uid" -> 1,
        "randomId" -> "2"
      )
      testToAndFromJson[SeqUpdateMessage](j, v)
    }

    "(de)serialize MessageSent" in {
      val v = MessageSent(1, 2)
      val j = withHeader(MessageSent.seqUpdateHeader)(
        "uid" -> 1,
        "randomId" -> "2"
      )
      testToAndFromJson[SeqUpdateMessage](j, v)
    }

    "(de)serialize NewDevice" in {
      val v = NewDevice(1, 2)
      val j = withHeader(NewDevice.seqUpdateHeader)(
        "uid" -> 1,
        "keyHash" -> "2"
      )
      testToAndFromJson[SeqUpdateMessage](j, v)
    }

    "(de)serialize NewYourDevice" in {
      val (key, keyJson) = genBitVector
      val v = NewYourDevice(1, 2, key)
      val j = withHeader(NewYourDevice.seqUpdateHeader)(
        "uid" -> 1,
        "keyHash" -> "2",
        "key" -> keyJson
      )
      testToAndFromJson[SeqUpdateMessage](j, v)
    }

  }

  "WeakUpdateMessage (de)serializer" should {

    "(de)serialize UserLastSeen" in {
      val v = UserLastSeen(1, 2)
      val j = withHeader(UserLastSeen.weakUpdateHeader)(
        "uid" -> 1,
        "time" -> "2"
      )
      testToAndFromJson[WeakUpdateMessage](j, v)
    }

    "(de)serialize UserOffline" in {
      val v = UserOffline(1)
      val j = withHeader(UserOffline.weakUpdateHeader)(
        "uid" -> 1
      )
      testToAndFromJson[WeakUpdateMessage](j, v)
    }

    "(de)serialize UserOnline" in {
      val v = UserOnline(1)
      val j = withHeader(UserOnline.weakUpdateHeader)(
        "uid" -> 1
      )
      testToAndFromJson[WeakUpdateMessage](j, v)
    }

  }

}
