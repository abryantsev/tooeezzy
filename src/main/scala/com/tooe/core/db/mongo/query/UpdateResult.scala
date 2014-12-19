package com.tooe.core.db.mongo.query

sealed trait UpdateResult

object UpdateResult {
  case object Updated extends UpdateResult
  case object NotFound extends UpdateResult
  case object NoUpdate extends UpdateResult
}