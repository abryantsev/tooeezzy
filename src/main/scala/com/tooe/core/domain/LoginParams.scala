package com.tooe.core.domain

import com.fasterxml.jackson.annotation.JsonProperty
import com.tooe.core.db.mongo.util.UnmarshallerEntity

case class LoginParams(@JsonProperty("username") userName: String, pwd: String) extends UnmarshallerEntity