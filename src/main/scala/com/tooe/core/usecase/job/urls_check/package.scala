package com.tooe.core.usecase.job

import com.tooe.core.domain.EntityType
import com.tooe.core.db.mongo.domain.Urls

package object urls_check {

  def getMediaServerSuffixByEntityType(url: Urls) =  url match {
    case url if url.entityType == EntityType.user && url.entityField == Some("um.bg") => "userbackgrounds"
    case url if url.entityType == EntityType.user && url.entityField == None => "users"
    case url if url.entityType == EntityType.user && url.entityField == Some("um.v") => "users_video"
    case url if url.entityType == EntityType.photo || url.entityType == EntityType.photoAlbum  => "photos"
    case url if url.entityType == EntityType.locationPhotoAlbum || url.entityType == EntityType.locationPhoto  => "locationphotos"
    case url if url.entityType == EntityType.product || url.entityType == EntityType.present => "products"
    case url if url.entityType == EntityType.location || url.entityType == EntityType.locationModeration || url.entityType == EntityType.locationsChain => "locations"
    case url if url.entityType == EntityType.company || url.entityType == EntityType.companyModeration => "companies"
    case url if url.entityType == EntityType.checkinLocation => "locations"
    case url if url.entityType == EntityType.checkinUser => "users"
  }

}
