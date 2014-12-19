package com.tooe.core.util

import xml.{XML, Utility, Node, Elem}

object XmlHelpers {
  implicit class ElemWrapper(val elem: Elem) extends AnyVal {
    def appendChild(c: Elem): Elem = appendChildren(c :: Nil)

    def appendChildren(cs: Traversable[Elem]): Elem = elem.copy(child = elem.child ++ cs)

    def trim: Node = Utility.trim(elem)
  }

  def tagValue(tag: String, value: String): Elem = XML.loadString(s"<$tag>$value</$tag>")

  implicit class XmlHelper(val source: String) extends AnyVal {
    def asXml = scala.xml.Utility.trim(XML.loadString(source))
  }
}
