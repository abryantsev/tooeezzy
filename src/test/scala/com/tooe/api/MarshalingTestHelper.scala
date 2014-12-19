package com.tooe.api

import com.tooe.core.db.mongo.util.JacksonModuleScalaSupport

trait MarshalingTestHelper extends JacksonModuleScalaSupport {

  import spray.httpx.marshalling._

  def entityToString[T: Marshaller](entity: T): String = marshal(entity).right.toOption.get.asString
}