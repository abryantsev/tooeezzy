package com.tooe.core.domain

import com.tooe.core.db.mongo.domain.Location

case class LocationWithDistance(location: Location, distance: Option[Double])