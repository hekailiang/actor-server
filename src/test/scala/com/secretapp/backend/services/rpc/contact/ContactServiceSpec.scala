package com.secretapp.backend.services.rpc.contact

import com.secretapp.backend.data.message.rpc.contact._
import com.secretapp.backend.services.rpc.RpcSpec
import com.secretapp.backend.data.message.struct
import scala.collection.immutable
import scalaz._
import Scalaz._

class ContactServiceSpec extends RpcSpec {
  implicit val transport = com.secretapp.backend.api.frontend.MTConnection // TODO

  "ContactService" should {
    "handle RPC import contacts request" in {
      implicit val scope = genTestScopeWithUser()
      val currentUser = scope.user
      val contact = genTestScopeWithUser().user
      val phones = immutable.Seq(
        PhoneToImport(contact.phoneNumber, contact.name.some),
        PhoneToImport(currentUser.phoneNumber, currentUser.name.some) /* check for filtered own phone number */
      )
      val emails = immutable.Seq[EmailToImport]()
      sendRpcMsg(RequestImportContacts(phones, emails))

      val (users, seq, state) = expectRpcMsgByPF(withNewSession = true) {
        case r: ResponseImportedContacts => (r.users, r.seq, r.state)
      }
      users.should_==(Seq(struct.User.fromModel(contact, scope.authId)))
    }
  }
}
