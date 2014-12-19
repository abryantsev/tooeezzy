package com.tooe.core.db.mongo.query

import org.springframework.data.domain.{Pageable, Sort}
import com.tooe.api.service.OffsetLimit

case class SkipLimitSort(skip: Int, limit: Int, sort: Sort = null) extends Pageable {


  def next(): Pageable = throw new IllegalStateException("next method is not supposed to be called")

  def previousOrFirst(): Pageable = throw new IllegalStateException("previousOrFirst method is not supposed to be called")

  def first(): Pageable = throw new IllegalStateException("first method is not supposed to be called")

  def hasPrevious: Boolean = throw new IllegalStateException("hasPrevious method is not supposed to be called")

  def getPageNumber: Int = throw new IllegalStateException("getPageNumber method is not supposed to be called")
  
  def getPageSize: Int = limit
  
  def getOffset: Int = skip
  
  def getSort: Sort = sort

  def sort(property: String, direction: Sort.Direction) = copy(sort = new Sort(direction, property) and sort)
  def asc(property: String) = sort(property, Sort.Direction.ASC)
  def desc(property: String) = sort(property, Sort.Direction.DESC)
}

object SkipLimitSort {
  def apply(offsetLimit: OffsetLimit): SkipLimitSort = SkipLimitSort(offsetLimit.offset, offsetLimit.limit)
  def apply(offsetLimit: OffsetLimit, sort: Sort): SkipLimitSort = SkipLimitSort(offsetLimit.offset, offsetLimit.limit, sort)
}