package com.tooe

import com.fasterxml.jackson.{annotation => jackson}
import scala.annotation.meta.{field, param}

package object api {

  type JsonProp = jackson.JsonProperty @field @param
  type JsonSkip = jackson.JsonIgnore @field
}