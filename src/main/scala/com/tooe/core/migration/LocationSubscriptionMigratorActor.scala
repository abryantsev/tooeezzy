package com.tooe.core.migration

import akka.pattern.{ask, pipe}
import com.tooe.core.db.mongo.util.UnmarshallerEntity
import com.tooe.core.usecase._
import com.tooe.core.migration.db.domain.MappingCollection._
import scala.concurrent.Future
import org.bson.types.ObjectId
import com.tooe.core.migration.db.domain.MappingCollection
import com.tooe.core.domain.{UserId, LocationId}
import com.tooe.core.migration.api.{DefaultMigrationResult, MigrationResponse}
import com.tooe.core.usecase.user.UserDataActor.UpdateStatistic
import com.tooe.core.usecase.location_subscription.LocationSubscriptionDataActor.CreateLocationSubscription
import com.tooe.core.db.mongo.domain.LocationSubscription
import com.tooe.core.usecase.location_subscription.LocationSubscriptionDataActor
import com.tooe.core.usecase.location.LocationDataActor
import com.tooe.core.db.mongo.domain.LocationSubscription
import com.tooe.core.migration.api.MigrationResponse
import com.tooe.core.migration.api.DefaultMigrationResult
import com.tooe.core.domain.LocationId
import com.tooe.core.usecase.location_subscription.LocationSubscriptionDataActor.CreateLocationSubscription
import com.tooe.core.usecase.user.UserDataActor

object LocationSubscriptionMigratorActor {
  val Id = 'locationSubscriptionMigratorActor
  implicit val ec = scala.concurrent.ExecutionContext.Implicits.global

  case class LegacyLocationSubscription(legacyid: Int, userids: Seq[Int]) extends UnmarshallerEntity
}

class LocationSubscriptionMigratorActor extends MigrationActor {

  import LocationSubscriptionMigratorActor._

  def receive = {
    case lls: LegacyLocationSubscription =>
      val future = for {
        locid <- lookupByLegacyId(lls.legacyid, MappingCollection.location).map(LocationId)
        usersCount <- saveSubscriptions(locid, lls.userids)
      } yield MigrationResponse(DefaultMigrationResult(lls.legacyid, new ObjectId(), "locationsubscription_migrator"))
      future pipeTo sender
  }

  def saveSubscriptions(lid: LocationId, users: Seq[Int]): Future[Int] = {
    getIdMappings(users, MappingCollection.user).mapInner(UserId).mapInner {
      uid =>
        locationSubscriptionDataActor ! CreateLocationSubscription(LocationSubscription(userId = uid, locationId = lid))
        updateStatisticActor ! UpdateStatisticActor.UserChangeLocationSubscriptionsCounter(uid, 1)
        updateStatisticActor ! UpdateStatisticActor.LocationChangeLocationSubscriptionsCounter(lid, 1)
    }.map(_.size)
  }

  lazy val locationSubscriptionDataActor = lookup(LocationSubscriptionDataActor.Id)
  lazy val locationDataActor = lookup(LocationDataActor.Id)
  lazy val updateStatisticActor = lookup(UpdateStatisticActor.Id)
  lazy val userReadActor = lookup(UserReadActor.Id)
  lazy val userDataActor = lookup(UserDataActor.Id)
  lazy val newsWriteActor = lookup(NewsWriteActor.Id)
}

