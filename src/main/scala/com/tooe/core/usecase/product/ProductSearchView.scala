package com.tooe.core.usecase.product

import com.tooe.core.db.mongo.util.{UnmarshallerEntity, HasIdentity, HasIdentityFactory}
import spray.httpx.unmarshalling.{MalformedContent, Deserializer, DeserializationError}

sealed trait ProductSearchView extends HasIdentity with UnmarshallerEntity{
  def id: String
}


object ProductSearchView extends HasIdentityFactory[ProductSearchView] {

  object Products extends ProductSearchView {
    def id = "products"
  }
  object ProductsCount extends ProductSearchView {
    def id = "productscount"
  }

  val values = Seq(Products, ProductsCount)
  private val idToVal = values.map(v => v.id -> v).toMap

  def get(id: String) = idToVal.get(id)
}