package com.tooe.core.db.mongo.domain

import com.tooe.core.util.Lang

/**
 * Has MongoDB representation like { k1:v1, k2:v2, ... }
 * @param map - internal representation, shouldn't be used directly
 * @tparam T - value type
 */
case class ObjectMap[T](map: Map[String, T])

object ObjectMap {
  def empty[T] = new ObjectMap[T](Map.empty)
  def apply[T](value: T)(implicit lang: Lang) = {
    val map = Map[String, T](lang.id -> value)
    if (lang == Lang.orig) map
    else map + (Lang.orig.id -> value)
  }

  implicit def toMap[T](objMap: ObjectMap[T]) = objMap.map
  implicit def fromStringMap[T](map: Map[String, T]) = ObjectMap[T](map)
  implicit def fromLangMap[T](map: Map[Lang, T]) = ObjectMap[T](map map { case (k,v) => k.id -> v })

  implicit class ObjectMapGetDefaultValueHelper[T](val obj: ObjectMap[T]) extends AnyVal {
    def getByLang(implicit lang: Lang): Option[T] = obj.get(lang.id)
    def localized(implicit lang: Lang): Option[T] = getByLang(lang) orElse getByLang(Lang.orig)

    @deprecated("use localized") def getWithDefaultKey(key: String, defaultKey: String = "orig") = obj.get(key) getOrElse obj(defaultKey)
    @deprecated("use localized") def getWithDefaultKeyOpt(key: String, defaultKey: String = "orig"): Option[T] = obj.get(key) orElse obj.get(defaultKey)
  }
}

case class MapKey(path: String*) {
  def key: String = path mkString "."
}

object MapKey {
  implicit def asString(mk: MapKey): String = mk.key
}