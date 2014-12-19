package com.tooe.core.db.mongo.converters

import com.tooe.core.domain.Coordinates

trait CoordinatesConverter {

  import DBObjectConverters._

  implicit val coordinatesConverter = new DBObjectConverter[Coordinates] {

    def serializeObj(obj: Coordinates) = DBObjectBuilder()
      .field("lon").value(obj.longitude)
      .field("lat").value(obj.latitude)

    def deserializeObj(source: DBObjectExtractor) = Coordinates(
      longitude = source.field("lon").value[Double](0.0),
      latitude = source.field("lat").value[Double](0.0)
    )

  }

}
