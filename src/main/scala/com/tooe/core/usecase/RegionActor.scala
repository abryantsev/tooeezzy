package com.tooe.core.usecase

import com.tooe.core.db.mongo.domain.{StatisticFields, Region}
import com.tooe.api.service.{SuccessfulResponse, RouteContext}
import com.tooe.core.domain.{RegionId, CountryId}
import com.tooe.core.application.Actors
import com.tooe.core.usecase.region.RegionDataActor
import com.tooe.core.util.Lang

object RegionActor {
  final val Id = Actors.Region

  case class GetRegionItemWithCountryId(id: RegionId, lang: Lang)
  case class GetRegionItem(id: RegionId, lang: Lang)
  case class GetRegions(countryId: CountryId, ctx: RouteContext)
  case class FindByStatistics(countryId: CountryId, statisticsFields: StatisticFields, ctx: RouteContext)
}

class RegionActor extends AppActor{

  import scala.concurrent.ExecutionContext.Implicits.global
  import RegionActor._

  lazy val regionDataActor = lookup(RegionDataActor.Id)

  def receive = {
    case GetRegionItemWithCountryId(id, lang) => getRegion(id, lang) map { region =>
      (RegionItem(lang)(region), region.countryId)
    } pipeTo sender

    case GetRegionItem(id, lang) => getRegion(id, lang) map RegionItem(lang) pipeTo sender

    case GetRegions(countryId, ctx) =>
      implicit val lang = ctx.lang
      regionDataActor.ask(RegionDataActor.GetRegionsByCountry(countryId)).mapTo[Seq[Region]].map {
        regions =>
          val (capital, sorted) = regions.distinct.sortBy(_.name.localized.getOrElse("")).partition(_.isCapital.isDefined)
          Regions((capital ++ sorted).map(r => RegionItem(r)))
      } pipeTo sender

    case FindByStatistics(countryId, statisticsFields, ctx) =>
      implicit val lang = ctx.lang
      (regionDataActor ? RegionDataActor.FindByStatistics(countryId, statisticsFields)).mapTo[List[Region]].map { regions =>
        val (capital, sorted) = regions.distinct.sortBy(_.name.localized.getOrElse("")).partition(_.isCapital.isDefined)
        Regions((capital ++ sorted).map(r => RegionItem(r)))
      } pipeTo sender
  }

  def getRegion(id: RegionId, lang: Lang) = (regionDataActor ? RegionDataActor.GetRegion(id)).mapTo[Region]
}

case class Regions(regions: Seq[RegionItem]) extends SuccessfulResponse

case class RegionItem(id: RegionId, name: String)

object RegionItem {
  def apply(lang: Lang)(region: Region): RegionItem = apply(region)(lang)

  def apply(region: Region)(implicit lang: Lang): RegionItem =
    RegionItem(region.id, region.name.localized.getOrElse(""))
}