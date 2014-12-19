package com.tooe.util.correction

import com.tooe.core.migration.MigrationActor
import com.tooe.core.infrastructure.BeanLookup
import com.tooe.core.service.{LocationPhotoAlbumDataService, LocationDataService}
import org.springframework.data.mongodb.core.MongoTemplate
import com.tooe.core.db.mongo.domain.{Location, PreModerationLocation}
import scala.collection.convert.WrapAsScala
import com.tooe.core.db.mongo.converters.LocationMediaConverter
import akka.actor.ActorLogging
import com.tooe.api.service.{OffsetLimit, SuccessfulResponse}
import org.springframework.data.mongodb.core.query.{Update, Criteria, Query}
import com.tooe.core.usecase.location.UpdateLocationStatistic

object LocationPhotoAndAlbumCorrector {
  val Id = 'locationPhotoAndAlbumCorrector

  case object FixLocationPhotosAndAlbums
}

class LocationPhotoAndAlbumCorrector extends MigrationActor with LocationMediaConverter with ActorLogging {

  import LocationPhotoAndAlbumCorrector._

  def receive = {
    case FixLocationPhotosAndAlbums =>
      val locations = WrapAsScala.asScalaBuffer(mongo.findAll(locationClass))
      locations.foreach {
        loc =>
          val albums = locationPhotoAlbumDataService.albumsByLocation(loc.locationId, OffsetLimit())

          albums.foreach {
            a =>
              locationDataService.addPhotoAlbumToLocation(a.locationId, a.id)
              log.info(s"Location ${a.locationId.id} got it's ${a.id} album.")
          }

          if (albums.length != loc.statistics.photoalbumsCount) {
            val value = albums.length - loc.statistics.photoalbumsCount
            locationDataService.changeStatistic(loc.id, UpdateLocationStatistic(photoAlbumsCount = Some(value)))
            log.info(s"Location ${loc.id.id} counter was corrected from ${loc.statistics.photoalbumsCount} to ${albums.length}")
            log.info(s"It was decreased from ${loc.statistics.photoalbumsCount} by increasing by $value")
          }

          log.info(s"Finished for location ${loc.id.id}")

      }
      sender ! SuccessfulResponse
  }

  def logLocation(loc: Location, pm: PreModerationLocation) {
    val s = s"Image swapped for location(${loc.id.id}) from (${loc.locationMedia.head}) to (${pm.locationMedia.head})"
    log.info(s)
  }

  val locationClass = classOf[Location]

  lazy val locationPhotoAlbumDataService = BeanLookup[LocationPhotoAlbumDataService]
  lazy val locationDataService = BeanLookup[LocationDataService]

  lazy val mongo = BeanLookup[LocationDataService].asInstanceOf[ {
    def mongo: MongoTemplate
  }].mongo
}

