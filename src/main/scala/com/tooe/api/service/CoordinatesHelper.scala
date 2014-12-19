package com.tooe.api.service

import spray.routing.Directives
import com.tooe.core.domain.Coordinates

trait CoordinatesHelper { self: Directives =>
  val coordinates = parameters('lon.as[Double], 'lat.as[Double]).as((lon: Double, lat: Double) => Coordinates(longitude = lon, latitude = lat))
}