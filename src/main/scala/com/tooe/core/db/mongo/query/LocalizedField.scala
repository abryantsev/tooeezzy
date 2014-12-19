package com.tooe.core.db.mongo.query
import com.tooe.core.util.Lang

case class LocalizedField(field: String, lang: Lang) {
  val value = s"$field.${lang.id}"
}
