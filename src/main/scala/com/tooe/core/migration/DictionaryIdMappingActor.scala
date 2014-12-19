package com.tooe.core.migration

import akka.pattern.{ask, pipe}
import com.tooe.core.usecase.AppActor
import com.tooe.core.migration.db.domain.MappingDictionary
import scala.concurrent.Future
import org.bson.types.ObjectId
import com.tooe.core.domain._
import com.tooe.core.migration.db.domain.DictionaryIdMapping
import com.tooe.core.domain.RegionId

object DictionaryIdMappingActor {

  final val Id = 'dictionaryIdMapping

  case class GetRegionId(legacy: Int)
  case class GetStarCategoryId(legacy: Int)
  case class GetUserGroup(legacy: Int)
  case class GetEventType(legacy: Int)
  case class GetRole(legacy: Int)
  case class GetLocationCategory(legacy: Int)
  case class GetCurrency(legacy: Int)
  case class GetPeriod(legacy: Option[Int])
}

class DictionaryIdMappingActor extends AppActor {

  import DictionaryIdMappingActor._
  import DictionaryIdMappingDataActor._
  import MappingDictionary._

  def mapId(legacy: => Int, dict: MappingDictionary): Future[DictionaryIdMapping] = {
    (dictionaryIdMappingDataActor ? GetDictionaryIdMapping(legacy, dict)).mapTo[DictionaryIdMapping]
  }

  def receive = {
    case GetRegionId(legacy) => mapId(legacy, region).map {
      case dim: DictionaryIdMapping =>
        val nid = dim.newId
        if (nid matches "ObjectID(.*)")
          RegionId(new ObjectId(nid.drop(9).init))
        else RegionId(new ObjectId(nid))
    } pipeTo sender

    case GetStarCategoryId(legacy) => mapId(legacy, starCategory).map {
      case dim: DictionaryIdMapping => StarCategoryId(dim.newId)
    } pipeTo sender

    case GetUserGroup(legacy) => mapId(legacy, usersGroup).map {
      case dim: DictionaryIdMapping => dim.newId
    }.fallbackTo(Future successful "") pipeTo sender

    case GetEventType(legacy) => mapId(legacy, eventType).map {
      case dim: DictionaryIdMapping => dim.newId
    } pipeTo sender

    case GetRole(legacy) => mapId(legacy, adminRole).map {
      case dim: DictionaryIdMapping => AdminRoleId(dim.newId)
    }.pipeTo(sender)

    case GetLocationCategory(legacy) => mapId(legacy, locationCategory).map {
      case dim => LocationCategoryId(dim.newId)
    }.pipeTo(sender)

    case GetCurrency(legacy) => mapId(legacy, currency).map {
      case dim => CurrencyId(dim.newId)
    }.pipeTo(sender)

    case GetPeriod(legacy) => val future =
      legacy match {
        case Some(id) => mapId(id, period).map(idm => PromotionPeriod.values.find(idm.newId == _.id).get)
        case None => Future successful PromotionPeriod.Default
      }
      future pipeTo sender
  }

  lazy val dictionaryIdMappingDataActor = lookup(DictionaryIdMappingDataActor.Id)
}