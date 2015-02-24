package im.actor.server.rest.models

import spray.json._

case class JsonListResponse(items: Seq[JsValue], totalCount: Int)
