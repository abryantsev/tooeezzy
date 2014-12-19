package com.tooe.core.payment.plantron

import xml._
import java.net.URLDecoder

object XmlMessageHelper {

  def parse(xml: Elem): PlatronParams =
    xml.child.flatMap(elem => elem.child match {
      case Seq(atom: Atom[Any]) => Seq(Symbol(elem.label) -> atom.data.toString)
      case Nil if !elem.isInstanceOf[Text] => Seq(Symbol(elem.label) -> "")
      case _ => Nil // ignore embedded tags
    })

  def build(root: Symbol, params: PlatronParams): String = {
    val rawParams = params map {
      case (k, v) =>
        val keyName = k.name
        s"<$keyName>$v</$keyName>"
    } mkString "\n"
    val rootName = root.name
    s"""<$rootName>
       |  $rawParams
       |</$rootName>""".stripMargin
  }

  def extractXmlRequest(body: String): Elem = {
    val rawXml = URLDecoder.decode(body, "UTF-8").replaceFirst("pg_xml=", "")
    XML.loadString(rawXml)
  }
}