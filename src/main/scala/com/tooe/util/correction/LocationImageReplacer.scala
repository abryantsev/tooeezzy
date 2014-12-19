package com.tooe.util.correction

import com.tooe.core.migration.MigrationActor
import com.tooe.core.infrastructure.BeanLookup
import com.tooe.core.service.LocationDataService
import org.springframework.data.mongodb.core.MongoTemplate
import com.tooe.core.db.mongo.domain.{Location, PreModerationLocation}
import scala.collection.convert.WrapAsScala
import com.tooe.core.domain.ModerationStatusId
import org.springframework.data.mongodb.core.query.{Update, Criteria, Query}
import com.tooe.core.db.mongo.converters.{LocationMediaConverter, DBObjectConverters}
import org.slf4j.Logger
import akka.actor.ActorLogging
import com.tooe.api.service.SuccessfulResponse
import java.io.{FileWriter, File}
import com.tooe.core.usecase.{ImageType, ImageInfo, DeleteMediaServerActor}

object LocationImageReplacer {
  val Id = 'locationImageReplacer

  case object FixLocationImages
}

class LocationImageReplacer extends MigrationActor with LocationMediaConverter with ActorLogging {

  import LocationImageReplacer._
  import com.tooe.core.db.mongo.query._
  import DBObjectConverters._

  def receive = {
    case FixLocationImages =>
      val locationMods = WrapAsScala.asScalaBuffer(mongo.findAll(modLocationClass))
      val filtered = locationMods.filter(pm => pm.locationMedia.nonEmpty && (pm.moderationStatus.status.id == ModerationStatusId.Active.id))
      filtered.foreach {
        pm =>
          val location = locationDataService.findOne(pm.publishedLocation.get).get
          try {
            if (pm.locationMedia.head != location.locationMedia.head) {
              logLocation(location, pm)
              val query = Query.query(new Criteria("id").is(location.id.id))
              val update = new Update()
                .setSerializeSeq("lm", pm.locationMedia)
              mongo.updateFirst(query, update, locationClass)

              mediaServerDeleteActor ! DeleteMediaServerActor
                .DeletePhotoFile(location.locationMedia.map(i => ImageInfo(name = i.url.url.id,
                ImageType.location, ownerId = location.id.id)))
            }
          } catch {
            case e: Exception => log.error(e, s"Could not update ${location.id}")
          }
      }
      sender ! SuccessfulResponse
  }

  def logLocation(loc: Location, pm: PreModerationLocation) {
    val s = s"Image swapped for location(${loc.id.id}) from (${loc.locationMedia.head}) to (${pm.locationMedia.head})"
    log.info(s)
  }

  val locationClass = classOf[Location]
  val modLocationClass = classOf[PreModerationLocation]

  lazy val mediaServerDeleteActor = lookup(DeleteMediaServerActor.Id)

  lazy val locationDataService = BeanLookup[LocationDataService]
  lazy val mongo = locationDataService.asInstanceOf[ {
    def mongo: MongoTemplate
  }].mongo
}
