package com.tooe.core.migration.api

import com.tooe.api.service.SuccessfulResponse
import org.bson.types.ObjectId

case class MigrationResponse
(
  migration: MigrationResult
  ) extends SuccessfulResponse

sealed trait MigrationResult {
  def migrator: String
}

case class DefaultMigrationResult
(
  legacyid: Long,
  newid: ObjectId,
  migrator: String
  ) extends MigrationResult

case class FriendshipMigrationResult
(
  legacyidA: Int,
  legacyidB: Int,
  newidA: ObjectId,
  newidB: ObjectId,
  migrator: String
  ) extends MigrationResult