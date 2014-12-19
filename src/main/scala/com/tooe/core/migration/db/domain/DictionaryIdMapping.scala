package com.tooe.core.migration.db.domain

import org.springframework.data.mongodb.core.mapping.Document
import org.bson.types.ObjectId
import com.tooe.core.migration.db.domain.MappingDictionary.MappingDictionary

@Document(collection = "idmapping_dic")
case class DictionaryIdMapping
(
  id: ObjectId = new ObjectId(),
  dictionary: MappingDictionary,
  legacyId: Int,
  newId: String
  )


object MappingDictionary extends Enumeration {

  type MappingDictionary = MappingDictionary.Value

  val maritalStatus = Value("maritalstatus")
  val country = Value("country")
  val currency = Value("currency")
  val eventType = Value("eventtype")
  val location = Value("location")
  val locationCategory = Value("location_category")
  val moderationStatus = Value("moderation_status")
  val region = Value("region")
  val period = Value("period")
  val starCategory = Value("star_category")
  val status = Value("status")
  val usersGroup = Value("usergroup")
  val adminRole = Value("adm_user_role")

}
