package com.tooe.core.domain

case class EntityType(id: String)

object EntityType {
  val user = EntityType("user")
  val photoAlbum = EntityType("photoalbum")
  val photo = EntityType("photo")
  val location = EntityType("location")
  val locationPhotoAlbum = EntityType("location_photoalbum")
  val locationPhoto = EntityType("location_photo")
  val locationsChain = EntityType("locationschain")
  val product = EntityType("product")
  val present = EntityType("present")
  val companyModeration = EntityType("company_mod")
  val company = EntityType("company")
  val locationModeration = EntityType("location_mod")
  val checkinLocation = EntityType("checkin_location")
  val checkinUser = EntityType("checkin_user")
}
