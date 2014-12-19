package com.tooe.core.usecase.promotion

import com.tooe.core.db.mongo.util.{UnmarshallerEntity, HasIdentity, HasIdentityFactory}
import org.springframework.data.domain.Sort

sealed trait PromotionSearchSortType extends HasIdentity with UnmarshallerEntity{
  def id: String
  def getSort(lang: String): Sort
}

object PromotionSearchSortType extends HasIdentityFactory[PromotionSearchSortType] {

  object Name extends PromotionSearchSortType {
    def id = "pmin"
    def getSort(lang: String): Sort = new Sort(lang)
  }

  object Distance extends PromotionSearchSortType {
    def id = "pmax"
    def getSort(lang: String): Sort = new Sort(Sort.DEFAULT_DIRECTION)
  }

  object Popularity extends PromotionSearchSortType {
    def id = "popularity"
    def getSort(lang: String): Sort = new Sort(Sort.DEFAULT_DIRECTION)
  }

  object None extends PromotionSearchSortType {
    def id = "none"
    def getSort(lang: String): Sort = Name.getSort(lang)
  }

  val values = Seq(Name, Distance, Popularity, None)
  private val idToVal = values.map(v => v.id -> v).toMap

  def get(id: String) = idToVal.get(id)

}