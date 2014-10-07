package com.secretapp.backend.data.json.transport

import com.secretapp.backend.data.json.CommonJsonFormats._
import com.secretapp.backend.data.transport._
import play.api.libs.json._

trait JsonFormats {

  import com.secretapp.backend.data.json.message._

  // FIXME: Doesn't work this way. Moved to the `message` namespace instead.
  // implicit val messageBoxFormat = Json.format[MessageBox]

  implicit val jsonPackageFormat = Json.format[JsonPackage]
  implicit val mtPackageFormat = Json.format[MTPackage]
  implicit val mtPackageBoxFormat = Json.format[MTPackageBox]
}
