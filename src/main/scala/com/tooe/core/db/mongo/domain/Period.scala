package com.tooe.core.db.mongo.domain

import com.tooe.core.domain.PeriodId
import org.springframework.data.mongodb.core.mapping.Document

@Document(collection = "period")
case class Period(id: PeriodId, name: ObjectMap[String])