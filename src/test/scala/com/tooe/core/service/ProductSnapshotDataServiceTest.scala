package com.tooe.core.service

import org.springframework.beans.factory.annotation.Autowired
import com.tooe.core.db.mysql.services.ProductSnapshotDataService
import org.junit.Test
import com.tooe.core.db.mysql.domain.ProductSnapshot
import java.util.UUID
import com.tooe.core.domain.CompanyId

class ProductSnapshotDataServiceTest extends SpringDataMySqlTestHelper {
  @Autowired var service: ProductSnapshotDataService = _

  @Test
  def readWrite() {
    val product = new ProductSnapshotFixture().product
    val result = service.save(product)
    val find = service.find(result.orderJpaId)

    find.map(_.productId) === Some(product.productId)
    find.map(_.product_type) === Some(product.product_type)
    find.map(_.name) === Some(product.name)
    find.map(_.descr) === Some(product.descr)
    find.map(_.currency) === Some(product.currency)
    find.map(_.validity) === Some(product.validity)
    find.map(_.companyId) === Some(product.companyId)
    find.map(_.loc_id) === Some(product.loc_id)
    find.map(_.loc_name) === Some(product.loc_name)
    find.map(_.loc_city) === Some(product.loc_city)
    find.map(_.loc_country) === Some(product.loc_country)
    find.map(_.loc_street) === Some(product.loc_street)
    find.map(_.picture_url) === Some(product.picture_url)
    find.map(_.present_msg) === Some(product.present_msg)
    find.map(_.article) === Some(product.article)

  }

  @Test
  def findAllByIds() {
    val products = (1 to 5).map(_ => service.save(new ProductSnapshotFixture().product)).toSeq
    service.findAllByIds(products.map(_.orderJpaId)).zip(products).foreach{
      case (found, expected) =>
        found.productId === expected.productId
        found.product_type === expected.product_type
        found.name === expected.name
        found.descr === expected.descr
        found.currency === expected.currency
        found.validity === expected.validity
        found.companyId === expected.companyId
        found.loc_id === expected.loc_id
        found.loc_name === expected.loc_name
        found.loc_city === expected.loc_city
        found.loc_country === expected.loc_country
        found.loc_street === expected.loc_street
        found.picture_url === expected.picture_url
        found.present_msg === expected.present_msg
        found.article === expected.article
    }
  }

}

class ProductSnapshotFixture {

  val product = ProductSnapshot(
    payment = new PaymentFixture().payment,
    productId = "some_product_id",
    product_type = "product",
    name = "product_name:" + UUID.randomUUID().toString,
    descr = "product_description",
    price = new java.math.BigDecimal(1000.toDouble),
    currency = "RUR",
    validity = 0,
    companyId = CompanyId().id.toString,
    loc_id = "locarion_id",
    loc_name = "location_name",
    loc_city = "location_city",
    loc_country = "location_country",
    loc_street = "location_street",
    picture_url = "picture_url",
    present_msg = "present_message",
    article = "product_article"
  )


}
