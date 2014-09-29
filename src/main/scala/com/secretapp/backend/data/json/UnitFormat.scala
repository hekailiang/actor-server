package com.secretapp.backend.data.json

import play.api.libs.json._

class UnitFormat[A](implicit m: scala.reflect.Manifest[A]) extends Format[A] {
  override def writes(o: A): JsValue = Json.obj()
  override def reads(json: JsValue): JsResult[A] = JsSuccess(m.runtimeClass.newInstance.asInstanceOf[A])
}

object UnitFormat {
  def apply[A](implicit m: scala.reflect.Manifest[A]): UnitFormat[A] = new UnitFormat[A]
}
