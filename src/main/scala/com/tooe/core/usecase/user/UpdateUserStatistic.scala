package com.tooe.core.usecase.user

import org.springframework.data.mongodb.core.query.Update
import com.tooe.core.usecase.user.UserStatisticsService._

case class UpdateUserStatistic(userEvents: Option[Int] = None,
                                newUserEvents: Option[Int] = None,
                                presents: Option[Int] = None,
                                certificates: Option[Int] = None,
                                sentPresents: Option[Int] = None,
                                sentCertificates: Option[Int] = None,
                                newPresents: Option[Int] = None,
                                friends: Option[Int] = None,
                                friendshipRequests: Option[Int] = None,
                                favoriteLocations: Option[Int] = None,
                                locationSubscriptions: Option[Int] = None,
                                userToStarSubscriptions: Option[Int] = None,
                                photoAlbums: Option[Int] = None,
                                wishes: Option[Int] = None,
                                fulfilledWishes: Option[Int] = None,
                                starsSubscriptionsCount: Option[Int] = None)

object UserStatisticsService {
  val userEventsCounterField = "st.ec"
  val newUserEventsCounterField = "st.new.ec"
  val presentsCounterField = "st.prc"
  val certificatesCounterField = "st.cc"
  val sentPresentsCounterField = "st.spc"
  val sentCertificatesCounterField = "st.scc"
  val newPresentsCounterField = "st.new.prc"
  val friendsCounterField = "st.fc"
  val friendshipRequestsCounterField = "st.frc"
  val favoriteLocationsCounterField = "st.flc"
  val userToStarSubscriptionsCounterField = "st.ssc"
  val photoAlbumsCounterField = "st.pc"
  val wishesCounterField = "st.wc"
  val fulfilledWishesCounterField = "st.fwc"
  val starsSubscriptionsCounterField = "star.suc"
  val locationSubscriptionsCounterField = "st.lsc"
}

object UpdateUserStatisticHelper {
  import com.tooe.core.util.BuilderHelper._
  def setStatistic(builder: Update, updater: UpdateUserStatistic) = builder
    .extend(updater.newUserEvents)(value => _.set(newUserEventsCounterField, value))
    .extend(updater.userEvents)(value => _.set(userEventsCounterField, value))
    .extend(updater.presents)(value => _.set(presentsCounterField, value))
    .extend(updater.newPresents)(value => _.set(newPresentsCounterField, value))
    .extend(updater.friends)(value => _.set(friendsCounterField, value))
    .extend(updater.friendshipRequests)(value => _.set(friendshipRequestsCounterField, value))
    .extend(updater.favoriteLocations)(value => _.set(favoriteLocationsCounterField, value))
    .extend(updater.userToStarSubscriptions)(value => _.set(userToStarSubscriptionsCounterField, value))
    .extend(updater.photoAlbums)(value => _.set(photoAlbumsCounterField, value))
    .extend(updater.wishes)(value => _.set(wishesCounterField, value))
    .extend(updater.fulfilledWishes)(value => _.set(fulfilledWishesCounterField, value))
    .extend(updater.certificates)(value => _.set(certificatesCounterField, value))
    .extend(updater.sentPresents)(value => _.set(sentPresentsCounterField, value))
    .extend(updater.sentCertificates)(value => _.set(sentCertificatesCounterField, value))
    .extend(updater.locationSubscriptions)(value => _.set(locationSubscriptionsCounterField, value))

  def changeStatistic(builder: Update, updater: UpdateUserStatistic) = builder
    .extend(updater.newUserEvents)(value => _.inc(newUserEventsCounterField, value))
    .extend(updater.userEvents)(value => _.inc(userEventsCounterField, value))
    .extend(updater.presents)(value => _.inc(presentsCounterField, value))
    .extend(updater.newPresents)(value => _.inc(newPresentsCounterField, value))
    .extend(updater.friends)(value => _.inc(friendsCounterField, value))
    .extend(updater.friendshipRequests)(value => _.inc(friendshipRequestsCounterField, value))
    .extend(updater.favoriteLocations)(value => _.inc(favoriteLocationsCounterField, value))
    .extend(updater.userToStarSubscriptions)(value => _.inc(userToStarSubscriptionsCounterField, value))
    .extend(updater.photoAlbums)(value => _.inc(photoAlbumsCounterField, value))
    .extend(updater.wishes)(value => _.inc(wishesCounterField, value))
    .extend(updater.fulfilledWishes)(value => _.inc(fulfilledWishesCounterField, value))
    .extend(updater.certificates)(value => _.inc(certificatesCounterField, value))
    .extend(updater.sentPresents)(value => _.inc(sentPresentsCounterField, value))
    .extend(updater.sentCertificates)(value => _.inc(sentCertificatesCounterField, value))
    .extend(updater.starsSubscriptionsCount)(value => _.inc(starsSubscriptionsCounterField, value))
    .extend(updater.locationSubscriptions)(value => _.inc(locationSubscriptionsCounterField, value))
}
