package com.tooe.core.db.mongo.domain

import org.springframework.data.mongodb.core.mapping.Document
import com.tooe.core.domain.MaritalStatusId
import com.fasterxml.jackson.annotation.JsonProperty

@Document(collection = "maritalstatus")
case class MaritalStatus(id: MaritalStatusId,
                         name: ObjectMap[String] = ObjectMap.empty,
                         @JsonProperty("nf")femaleStatusName: ObjectMap[String] = ObjectMap.empty)