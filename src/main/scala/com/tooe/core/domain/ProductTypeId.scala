package com.tooe.core.domain

case class ProductTypeId(id: String)

object ProductTypeId {
  val Certificate = ProductTypeId("certificate")
  val Product = ProductTypeId("product")
}