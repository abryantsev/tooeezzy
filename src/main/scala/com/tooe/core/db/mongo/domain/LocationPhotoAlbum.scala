package com.tooe.core.db.mongo.domain

import org.springframework.data.mongodb.core.mapping.Document
import com.tooe.core.domain.{MediaObject, LocationsChainId, LocationPhotoAlbumId, LocationId}

@Document(collection = "location_photoalbum")
case class LocationPhotoAlbum
(
  id: LocationPhotoAlbumId = new LocationPhotoAlbumId(),
  locationId: LocationId,
  name: String,
  description: Option[String] = None,
  photosCount: Int = 0,
  frontPhotoUrl: MediaObject,
  locationsChainId: Option[LocationsChainId]
  )