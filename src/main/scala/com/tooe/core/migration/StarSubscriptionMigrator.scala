package com.tooe.core.migration

import akka.pattern.{ask, pipe}
import com.tooe.core.usecase.AppActor
import com.tooe.core.db.mongo.util.UnmarshallerEntity
import com.tooe.core.usecase.star_subscription.StarSubscriptionDataActor
import scala.concurrent.Future
import org.bson.types.ObjectId
import com.tooe.core.migration.db.domain.MappingCollection
import com.tooe.core.usecase.star_subscription.StarSubscriptionDataActor.AddSubscription
import com.tooe.core.domain.UserId
import com.tooe.core.migration.api.{DefaultMigrationResult, MigrationResponse}

object StarSubscriptionMigrator {

  val Id = 'starSubscriptionMigrator

  case object NotApplicable extends ObjectId
  case class LegacyStarSubscribe(legacyid: Int, userids: Seq[Int]) extends UnmarshallerEntity
}

class StarSubscriptionMigrator extends MigrationActor {

  import StarSubscriptionMigrator._

  def receive = {
    case lce: LegacyStarSubscribe =>
      val result = for {
        starId <- lookupByLegacyId(lce.legacyid, MappingCollection.user).map(UserId)
        userIds <- getIdMappings(lce.userids, MappingCollection.user).mapInner(UserId)
        _ <- Future.traverse(userIds)(starSubscriptionDataActor ? AddSubscription(_, starId))
      } yield MigrationResponse(DefaultMigrationResult(lce.legacyid, NotApplicable, "starsubscription_migrator"))
      result pipeTo sender
  }

  lazy val starSubscriptionDataActor = lookup(StarSubscriptionDataActor.Id)
}
