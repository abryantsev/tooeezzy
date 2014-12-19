package com.tooe.core.db.mysql.domain

import java.math.{BigInteger, BigDecimal}
import javax.persistence.Entity
import com.tooe.core.domain.{CompanyId, ProductTypeId, LocationId, ProductId}
import org.bson.types.ObjectId

@Entity(name = "products")
case class ProductSnapshot
(
  @MapsId
  @OneToOne(mappedBy = "productSnapshot")
  @JoinColumn(name = "order_id")
  payment: Payment,

  @Column(name = "product_id", nullable = false)
  productId: String = null,

  @Column(name = "product_type", nullable = false)
  product_type: String = null,

  @Column(name = "name", nullable = false)
  name: String = null,

  @Column(name = "descr", nullable = false)
  descr: String = null,

  @Column(name = "price", nullable = false)
  price: BigDecimal = null,

  @Column(name = "currency", nullable = false)
  currency: String = null,

  @Column(name = "validity", nullable = false)
  validity: Int = 0,

  @Column(name = "company_id", nullable = false)
  companyId: String = null,

  @Column(name = "loc_id", nullable = false)
  loc_id: String = null,

  @Column(name = "loc_name", nullable = false)
  loc_name: String = null,

  @Column(name = "loc_city", nullable = false)
  loc_city: String = null,

  @Column(name = "loc_country", nullable = false)
  loc_country: String = null,

  @Column(name = "loc_street", nullable = false)
  loc_street: String = null,
  
  @Column(name = "url", nullable = true)
  picture_url: String = null,
  
  @Column(name = "present_msg", nullable = true)
  present_msg: String = null,

  @Column(name = "article", nullable = true)
  article: String = null

  ) extends HasOrderId
{
  def this() = this(null)

  if (payment != null) payment.productSnapshot = this

  @Id
  @Column(name = "order_id", nullable = false)
  var orderJpaId: BigInteger = _

  def getPictureUrl = Option(picture_url)

  def getLocationId = LocationId(new ObjectId(loc_id))

  def getCompanyId = CompanyId(new ObjectId(companyId))

  def getProductId = ProductId(new ObjectId(productId))

  def getMessage = Option(present_msg)

  def getProductTypeId = ProductTypeId(product_type)

  def getArticle = Option(article)
}