package com.tooe.core.service

sealed trait DeleteResult

object DeleteResult {
  object Deleted extends DeleteResult
}