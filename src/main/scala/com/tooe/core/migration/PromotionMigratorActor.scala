package com.tooe.core.migration

import akka.pattern.{ask, pipe}
import com.tooe.core.usecase.{UpdateStatisticActor, AppActor}
import java.util.Date
import com.tooe.core.db.mongo.util.UnmarshallerEntity
import scala.concurrent.Future
import com.tooe.core.domain._
import org.bson.types.ObjectId
import com.tooe.core.util.Lang
import com.tooe.core.db.mongo.domain._
import com.tooe.core.migration.db.domain.MappingCollection
import com.tooe.core.usecase.location.LocationDataActor
import com.tooe.core.usecase.promotion.{PromotionVisitorDataActor, PromotionDataActor}
import com.tooe.core.db.mongo.domain.Promotion
import com.tooe.core.domain.PromotionId
import com.tooe.core.db.mongo.domain.Location
import com.tooe.core.domain.LocationId
import com.tooe.core.db.mongo.domain.PromotionVisitor
import com.tooe.core.domain.MediaUrl
import com.tooe.core.db.mongo.domain.promotion.Dates
import com.tooe.core.migration.db.domain.IdMapping
import com.tooe.core.db.mongo.query.UpdateResult
import com.tooe.core.migration.api.{DefaultMigrationResult, MigrationResponse}

object PromotionMigratorActor {
  val Id = 'PromoteMigratorActor

  case class LegacyVisitor(uid: Int, time: Date, status: String)
  case class LegacyPromotion(legacyid: Int, name: String, description: String,
                             addinfo: Option[String], startdate: Date, enddate: Option[Date],
                             time: Option[Date], period: Option[Int], price: Option[String],
                             locationid: Int, media: Option[String], visitors: Seq[LegacyVisitor]) extends UnmarshallerEntity

  implicit def str2ObjMap(str: String): ObjectMap[String] = Map(Lang.ru -> str)
  implicit def str2ObjMapOptLifted(os: Option[String]) = os map str2ObjMap
}

class PromotionMigratorActor extends MigrationActor {
  import PromotionMigratorActor._

  def receive = {
    case lp: LegacyPromotion =>
      savePromote(lp) map {
        promo =>
          idMappingDataActor ! IdMappingDataActor.SaveIdMapping(IdMapping(new ObjectId(), MappingCollection.promotion, lp.legacyid, promo.id.id))
          MigrationResponse(DefaultMigrationResult(lp.legacyid, promo.id.id, "promotion_migrator"))
      } pipeTo sender
  }

  def savePromote(lp: LegacyPromotion): Future[Promotion] = {
    def savePromotionVisitors(promo: Promotion): Future[Seq[UpdateResult]] = {
      def updateVisitor(lv: LegacyVisitor): Future[PromotionVisitor] =
        for {
          uid <- lookupByLegacyId(lv.uid, MappingCollection.user).map(UserId)
        } yield PromotionVisitor(PromotionVisitorId(new ObjectId), promo.id, uid, PromotionStatus.Confirmed, lv.time)
      Future.traverse(lp.visitors) {
        lv =>
          promotionDataActor ! PromotionDataActor.IncrementVisitorsCounter(promo.id, 1)
          updateVisitor(lv).flatMap(visitor =>
            (promotionVisitorDataActor ? PromotionVisitorDataActor.UpsertStatus(visitor.promotion, visitor.visitor, visitor.time, visitor.status)).mapTo[UpdateResult]
          )
      }
    }
    def findLocationId: Future[LocationId] = {
      lookupByLegacyId(lp.locationid, MappingCollection.location).map(LocationId)
    }
    def findLocation(lid: LocationId): Future[Location] = {
      (locationDataActor ? LocationDataActor.GetLocation(lid)).mapTo[Location]
    }
    def makePromotion(loc: Location, period: PromotionPeriod): Promotion = {
      Promotion(PromotionId(new ObjectId),
        name = lp.name,
        description = lp.description,
        additionalInfo = lp.addinfo,
        media = lp.media.map(MediaUrl).toSeq,
        dates = Dates(lp.startdate, lp.enddate, lp.time, period = period),
        price = lp.price,
        location = promotion.Location(loc.id, loc.name, loc.contact.address.regionId, loc.locationCategories),
        visitorsCount = 0
      )
    }
    for {
      lid <- findLocationId
      loc <- findLocation(lid)
      period <- (dictMappingActor ? DictionaryIdMappingActor.GetPeriod(lp.period)).mapTo[PromotionPeriod]
      promo = makePromotion(loc, period)
      aa <- promotionDataActor ? PromotionDataActor.SavePromotion(promo)
      visitors <- savePromotionVisitors(promo)
    } yield {
      updateStatisticActor ! UpdateStatisticActor.ChangePromotionsCounter(loc.contact.address.regionId, 1)
      locationDataActor ! LocationDataActor.UpdatePromotionsFlag(lid, Some(true))
      promo
    }
  }

  lazy val dictMappingActor = lookup(DictionaryIdMappingActor.Id)
  lazy val promotionVisitorDataActor = lookup(PromotionVisitorDataActor.Id)
  lazy val updateStatisticActor = lookup(UpdateStatisticActor.Id)
  lazy val promotionDataActor = lookup(PromotionDataActor.Id)
  lazy val locationDataActor = lookup(LocationDataActor.Id)
}