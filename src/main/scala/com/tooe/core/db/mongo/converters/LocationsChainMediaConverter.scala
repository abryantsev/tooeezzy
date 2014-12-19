package com.tooe.core.db.mongo.converters

import com.tooe.core.domain.MediaObject
import com.tooe.core.db.mongo.domain.LocationsChainMedia

trait LocationsChainMediaConverter extends MediaObjectConverter {
  import DBObjectConverters._

  implicit val locationsChainMediaConverter = new DBObjectConverter[LocationsChainMedia] {

    def serializeObj(obj: LocationsChainMedia) = DBObjectBuilder()
      .field("u").value(obj.media)

    def deserializeObj(source: DBObjectExtractor) =
      LocationsChainMedia(source.field("u").value[MediaObject])

  }
}
