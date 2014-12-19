package com.tooe.api.marshalling

import com.tooe.core.application.Implementation
import spray.httpx.SprayJsonSupport
import spray.json.DefaultJsonProtocol
import com.tooe.core.db.mongo.domain.BasicUser
import org.bson.types.ObjectId
import spray.json.JsonFormat
import spray.json.JsValue
import spray.json.JsString
import spray.json._
import com.tooe.core.application._
import com.tooe.api.service.SystemInfo

// might move upstream: https://github.com/spray/spray-json/issues/24
@deprecated("expecting to move upstream", "") trait UuidMarshalling {
  implicit object UuidJsonFormat extends JsonFormat[ObjectId] {
    def write(x: ObjectId) = JsString(x toString ())
    def read(value: JsValue) = value match {
      case JsString(x) => ObjectId.massageToObjectId(x)
      case x => deserializationError("Expected UUID as JsString, but got " + x)
    }
  }
}

trait Marshalling extends DefaultJsonProtocol with UuidMarshalling with SprayJsonSupport {

  implicit val ImplementationFormat = jsonFormat3(Implementation)
  implicit val SystemInfoFormat = jsonFormat3(SystemInfo)

  implicit val BasicUserFormat = jsonFormat3(BasicUser)
}
