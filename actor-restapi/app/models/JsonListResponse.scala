package models

import play.api.libs.json.JsValue

case class JsonListResponse(items: Seq[JsValue], totalCount: Int)
