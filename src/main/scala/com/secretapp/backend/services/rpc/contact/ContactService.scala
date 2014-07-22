package com.secretapp.backend.services.rpc.contact

import akka.actor._
import com.secretapp.backend.data.message.rpc.contact.{ContactToImport, RequestImportContacts}
import com.secretapp.backend.services.common.PackageCommon
import com.secretapp.backend.services.common.PackageCommon._
import com.secretapp.backend.services.GeneratorService
import com.secretapp.backend.data.message.rpc._
import com.secretapp.backend.data.transport._
import com.secretapp.backend.services.rpc.RpcCommon
import com.datastax.driver.core.{ Session => CSession }
import scala.collection.immutable
import scalaz._
import Scalaz._

trait ContactService extends PackageCommon with RpcCommon { self: Actor with GeneratorService =>
  implicit val session: CSession

  import context._

//  def handleRpcContact(p: Package, messageId: Long): PartialFunction[RpcRequestMessage, Any] = {
//    case r: RequestImportContacts =>
//      sendRpcResult(p, messageId)((handleRequestImportContacts _).tupled(RequestImportContacts.unapply(r).get))
//  }
//
//  def handleRequestImportContacts(contacts: immutable.Seq[ContactToImport]): RpcResult = ???
}
