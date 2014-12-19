package com.tooe.core.usecase.product

import com.tooe.core.db.mongo.util.{UnmarshallerEntity, HasIdentity, HasIdentityFactory}
import org.springframework.data.domain.Sort
import com.tooe.core.db.mongo.domain.MapKey
import org.springframework.data.domain.Sort.Order

sealed trait ProductSearchSortType extends HasIdentity with UnmarshallerEntity{
  def id: String
  def getSort(lang: String): Sort
}


object ProductSearchSortType extends HasIdentityFactory[ProductSearchSortType] {

  object Name extends ProductSearchSortType {
    def id = "name"
    def getSort(lang: String): Sort = new Sort(MapKey("n", lang).key)
  }

  object PMin extends ProductSearchSortType {
    def id = "pmin"
    def getSort(lang: String): Sort = new Sort(new Order(Sort.Direction.ASC, "p.v"))
  }

  object PMax extends ProductSearchSortType {
    def id = "pmax"
    def getSort(lang: String): Sort = new Sort(new Order(Sort.Direction.DESC, "p.v"))
  }

  object Popularity extends ProductSearchSortType {
    def id = "popularity"
    def getSort(lang: String): Sort = new Sort(new Order(Sort.Direction.DESC, "c"))
  }

  object None extends ProductSearchSortType {
    def id = "none"
    def getSort(lang: String): Sort = Name.getSort(lang)
  }

  val values = Seq(Name, PMin, PMax, Popularity, None)
  private val idToVal = values.map(v => v.id -> v).toMap

  def get(id: String) = idToVal.get(id)

  implicit def SortingHelper(sortType: Option[ProductSearchSortType]) = new {
    def toSort(lang: String) = sortType.map(_.getSort(lang)).getOrElse(ProductSearchSortType.Name.getSort(lang))
  }


}