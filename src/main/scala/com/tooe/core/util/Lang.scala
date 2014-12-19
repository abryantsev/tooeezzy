package com.tooe.core.util

case class Lang(id: String) {
  override def toString = id
}

object Lang {
  val orig = Lang("orig")
  val ru = Lang("ru")
  
  implicit def toId(lang: Lang): String = lang.id
}