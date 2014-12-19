package com.tooe.core.db.mongo.domain

import org.springframework.data.mongodb.core.mapping.Document
import com.tooe.core.domain._
import java.util.Date

@Document(collection = "locationschain")
case class LocationsChain
(
  id: LocationsChainId,
  name: ObjectMap[String],
  description: Option[ObjectMap[String]],
  companyId: CompanyId,
  registrationDate: Date,
  locationCount: Int,
  locationChainMedia: Seq[LocationsChainMedia]
  )

case class LocationsChainMedia(media: MediaObject)