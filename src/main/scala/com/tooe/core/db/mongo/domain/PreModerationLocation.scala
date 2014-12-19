package com.tooe.core.db.mongo.domain

import org.springframework.data.mongodb.core.mapping.Document
import com.tooe.core.domain._
import com.tooe.core.db.mongo.util.PermutationHelper

@Document(collection = "location_mod")
case class PreModerationLocation
(
  id: PreModerationLocationId = PreModerationLocationId(),
  companyId: CompanyId,
  locationsChainId: Option[LocationsChainId],
  name: ObjectMap[String] = ObjectMap.empty,
  description: ObjectMap[String] = ObjectMap.empty,
  openingHours: ObjectMap[String] = ObjectMap.empty,
  contact: LocationContact,
  locationCategories: Seq[LocationCategoryId] = Nil,
  additionalLocationCategories: Seq[AdditionalLocationCategory] = Nil,
  locationMedia: Seq[LocationMedia] = Nil,
  lifecycleStatusId: Option[LifecycleStatusId],
  publishedLocation: Option[LocationId] = None,
  moderationStatus: PreModerationStatus = PreModerationStatus()
  ) {

  lazy val names = name.values.flatMap(PermutationHelper.permuteBySpace).toSeq

}

