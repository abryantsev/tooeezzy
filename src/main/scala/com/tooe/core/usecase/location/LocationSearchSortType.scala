package com.tooe.core.usecase.location

import com.tooe.core.db.mongo.util.{HasIdentityFactoryEx, UnmarshallerEntity, HasIdentity}
import org.springframework.data.domain.Sort
import com.tooe.core.util.Lang


sealed trait LocationSearchSortType extends HasIdentity with UnmarshallerEntity{
  def id: String
  def getSort(lang: Lang): Option[Sort]
}

object LocationSearchSortType extends HasIdentityFactoryEx[LocationSearchSortType] {

  object Name extends LocationSearchSortType {
    def id = "name"
    def getSort(lang: Lang): Option[Sort] = Some(new Sort(Sort.Direction.ASC, s"n.${lang.id}"))
  }

  object Distance extends LocationSearchSortType {
    def id = "distance"
    def getSort(lang: Lang) = None
  }

  object Popularity extends LocationSearchSortType {
    def id = "popularity"
    def getSort(lang: Lang): Option[Sort] = Some(new Sort(Sort.Direction.ASC, "sc"))
  }

  val values = Seq(Name, Distance, Popularity)

}