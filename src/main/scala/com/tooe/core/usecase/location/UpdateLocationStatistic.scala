package com.tooe.core.usecase.location

import org.springframework.data.mongodb.core.query.Update

case class UpdateLocationStatistic(
  productsCount: Option[Int] = None,
  photoAlbumsCount: Option[Int] = None,
  reviewsCount: Option[Int] = None,
  favoritePlacesCount: Option[Int] = None,
  presentsCount: Option[Int] = None,
  subscribersCount: Option[Int] = None,
  checkinsCount: Option[Int] = None
)
object UpdateLocationStatistic {
  val productsCounterField = "st.prc"
  val photoAlbumsCounterField = "st.pac"
  val reviewsCounterField = "st.rc"
  val favoritePlacesCounterField = "st.fc"
  val presentsCounterField = "st.pc"
  val subscribersCounterField = "st.sc"
  val checkinsCounterField = "st.cc"
}

object UpdateLocationStatisticHelper {
  import com.tooe.core.util.BuilderHelper._
  import UpdateLocationStatistic._
  def setStatistic(builder: Update, updater: UpdateLocationStatistic) = builder
    .extend(updater.productsCount)(value => _.set(productsCounterField, value))
    .extend(updater.photoAlbumsCount)(value => _.set(photoAlbumsCounterField, value))
    .extend(updater.reviewsCount)(value => _.set(reviewsCounterField, value))
    .extend(updater.favoritePlacesCount)(value => _.set(favoritePlacesCounterField, value))
    .extend(updater.presentsCount)(value => _.set(presentsCounterField, value))
    .extend(updater.subscribersCount)(value => _.set(subscribersCounterField, value))
    .extend(updater.checkinsCount)(value => _.set(checkinsCounterField, value))

  def changeStatistic(builder: Update, updater: UpdateLocationStatistic) = builder
    .extend(updater.productsCount)(value => _.inc(productsCounterField, value))
    .extend(updater.photoAlbumsCount)(value => _.inc(photoAlbumsCounterField, value))
    .extend(updater.reviewsCount)(value => _.inc(reviewsCounterField, value))
    .extend(updater.favoritePlacesCount)(value => _.inc(favoritePlacesCounterField, value))
    .extend(updater.presentsCount)(value => _.inc(presentsCounterField, value))
    .extend(updater.subscribersCount)(value => _.inc(subscribersCounterField, value))
    .extend(updater.checkinsCount)(value => _.inc(checkinsCounterField, value))

}

