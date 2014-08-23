package com.secretapp.backend.services.rpc

import com.secretapp.backend.data.message.{ RpcResponseBox, TransportMessage }
import com.secretapp.backend.data.message.rpc.Ok
import com.secretapp.backend.data.transport.MessageBox
import org.specs2.matcher.ShouldMatchers
import scala.reflect.ClassTag

trait RpcSpecAssertions extends ShouldMatchers {
  implicit class AnyRefWithSpecAssertions(x: AnyRef) {
    def assertInstanceOf[A: ClassTag]: A = {
      x should beAnInstanceOf[A]
      x.asInstanceOf[A]
    }
  }

  implicit class MessageBoxWithSpecAssertions(x: MessageBox) {
    def assertResponseOk[A: ClassTag]: A = {
      x
        .body.assertInstanceOf[RpcResponseBox]
        .body.assertInstanceOf[Ok]
        .body.assertInstanceOf[A]
    }
  }
}
