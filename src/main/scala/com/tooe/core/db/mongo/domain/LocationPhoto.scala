package com.tooe.core.db.mongo.domain

import org.springframework.data.mongodb.core.mapping.Document
import org.bson.types.ObjectId
import org.springframework.data.annotation.Id
import java.util.Date
import com.tooe.core.domain._
import com.tooe.core.usecase.{ImageType, ImageInfo}
import com.tooe.core.domain.LocationId
import com.tooe.core.domain.UserId
import com.tooe.core.domain.LocationPhotoAlbumId
import com.tooe.core.domain.LocationPhotoId


@Document(collection = "location_photo")
case class LocationPhoto(
                          id: LocationPhotoId = LocationPhotoId(),
                          photoAlbumId: LocationPhotoAlbumId,
                          locationId: LocationId,
                          creationDate: Date = new Date(),
                          name: Option[String] = None,
                          fileUrl: MediaObject,
                          likesCount: Int = 0,
                          usersLikesIds: Seq[UserId] = Nil,
                          commentsCount: Int = 0,
                          comments: Seq[UserId] = Nil
                          )

