package com.tooe.core.domain

import com.fasterxml.jackson.annotation.JsonProperty
import com.tooe.core.db.mongo.domain._
import com.javadocmd.simplelatlng.{LatLngTool, LatLng}
import com.javadocmd.simplelatlng.util.LengthUnit

case class Coordinates
(
  @JsonProperty("lon") @Field("lon") longitude: Double = 0.0,
  @JsonProperty("lat") @Field("lat") latitude: Double = 0.0
) {

  def distanceKm(that: Coordinates): Double =
    LatLngTool.distance(new LatLng(latitude, longitude), new LatLng(that.latitude, that.longitude), LengthUnit.KILOMETER)
}

object Coordinates {
 implicit def coordinatesToLatLng(coordinates: Coordinates) = new {
  def toLatLng = new LatLng(coordinates.latitude, coordinates.longitude)
 }
}
