package com.tooe.core.migration.db.domain

import org.springframework.data.mongodb.core.mapping.Document
import org.bson.types.ObjectId
import com.tooe.core.migration.db.domain.MappingCollection.MappingCollection

@Document(collection = "idmapping")
case class IdMapping
(
  id: ObjectId = new ObjectId(),
  collection: MappingCollection,
  legacyId: Int,
  newId: ObjectId,
  ownerNewId: Option[ObjectId] = None
  )


object MappingCollection extends Enumeration {

  type MappingCollection = MappingCollection.Value

  val friendshipRequest = Value("friendshipRequest")
  val userPhotoAlbum = Value("userPhotoAlbum")
  val wish = Value("wish")
  val locationsChain = Value("locationsChain")
  val userPhoto = Value("userPhoto")
  val present = Value("present")
  val promotion = Value("promo")
  val user = Value("user")
  val admUser = Value("adm_user")
  val company = Value("company")
  val location = Value("location")
  val locationAddCategories = Value("location_add_categories")
  val product = Value("product")
}
