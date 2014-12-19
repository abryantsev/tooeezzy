package com.tooe.core.db.mongo.converters

trait DBCommonConverters
  extends MediaUrlConverter
  with MediaObjectConverter
  with PhotoShortConverter

object DBCommonConverters extends DBCommonConverters

