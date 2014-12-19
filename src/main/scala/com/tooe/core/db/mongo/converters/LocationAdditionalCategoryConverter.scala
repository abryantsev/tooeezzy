package com.tooe.core.db.mongo.converters

import com.tooe.core.db.mongo.domain.AdditionalLocationCategory
import com.tooe.core.domain.AdditionalLocationCategoryId

trait LocationAdditionalCategoryConverter {

  import DBObjectConverters._

  implicit val locationAdditionalCategoryConverter = new DBObjectConverter[AdditionalLocationCategory] {
    def serializeObj(obj: AdditionalLocationCategory) = DBObjectBuilder()
      .field("cid").value(obj.id)
      .field("n").value(obj.name)

    def deserializeObj(source: DBObjectExtractor) = AdditionalLocationCategory(
      id = source.field("cid").value[AdditionalLocationCategoryId],
      name = source.field("n").objectMap[String]
    )
  }


}
