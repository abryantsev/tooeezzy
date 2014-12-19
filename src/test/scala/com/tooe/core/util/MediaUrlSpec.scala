package com.tooe.core.util

import org.specs2.mutable.SpecificationWithJUnit
import org.specs2.matcher.MustMatchers
import com.tooe.core.service.UrlsFixture
import com.tooe.core.domain.EntityType

class MediaUrlSpec extends SpecificationWithJUnit with MustMatchers {

  "Media url" should {

    "correct load type by EntityType" >> {

      val url = new UrlsFixture().url

      val expected = Map( url.copy(entityType = EntityType.user, entityField = None) -> "users",
                          url.copy(entityType = EntityType.user, entityField = Some("um.bg")) -> "userbackgrounds",
                          url.copy(entityType = EntityType.user, entityField = Some("um.v")) -> "users_video",
                          url.copy(entityType = EntityType.photo) -> "photos",
                          url.copy(entityType = EntityType.photoAlbum) -> "photos",
                          url.copy(entityType = EntityType.locationPhotoAlbum) -> "locationphotos",
                          url.copy(entityType = EntityType.locationPhoto) -> "locationphotos",
                          url.copy(entityType = EntityType.product) -> "products",
                          url.copy(entityType = EntityType.present) -> "products",
                          url.copy(entityType = EntityType.location) -> "locations",
                          url.copy(entityType = EntityType.locationModeration) -> "locations",
                          url.copy(entityType = EntityType.locationsChain) -> "locations",
                          url.copy(entityType = EntityType.company) -> "companies",
                          url.copy(entityType = EntityType.companyModeration) -> "companies",
                          url.copy(entityType = EntityType.checkinLocation) -> "locations",
                          url.copy(entityType = EntityType.checkinUser) -> "users")

      import com.tooe.core.usecase.job.urls_check._
      for((u, v) <- expected) getMediaServerSuffixByEntityType(u) === v

    }

  }

}
