package com.tooe.core.db.mongo.query

sealed trait UpsertResult

object UpsertResult {
  case object Inserted extends UpsertResult
  case object Updated extends UpsertResult
}