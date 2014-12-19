package com.tooe.core.db.mongo.converters

import com.tooe.core.db.mongo.domain.PresentProduct
import com.tooe.core.domain.{CompanyId, ProductTypeId, LocationId, ProductId}
import com.tooe.core.db.mongo.domain.PresentProductMedia

trait PresentProductConverter extends PresentProductMediaConverter {

  import DBObjectConverters._

  implicit val presentProductConverter = new DBObjectConverter[PresentProduct] {
    def serializeObj(obj: PresentProduct) = DBObjectBuilder()
      .field("cid").value(obj.companyId)
      .field("lid").value(obj.locationId)
      .field("pid").value(obj.productId)
      .field("pn").value(obj.productName)
      .field("pm").value(obj.media)
      .field("pt").value(obj.productTypeId)
      .field("d").value(obj.description)
      
    def deserializeObj(source: DBObjectExtractor) = PresentProduct(
      companyId = source.field("cid").value[CompanyId],
      locationId = source.field("lid").value[LocationId],
      productId = source.field("pid").value[ProductId],
      productName = source.field("pn").value[String],
      media = source.field("pm").opt[PresentProductMedia],
      productTypeId = source.field("pt").value[ProductTypeId],
      description = source.field("d").value[String]
    )
  }
}