package com.tooe.core.payment.plantron

import com.tooe.core.util.HashHelper
import scala.Some

object PlatronMessageSigner {

  private val PgSig = Symbol("pg_sig")

  def paramsToSign(pp: PlatronParams): PlatronParams = pp filterNot (_._1 == PgSig) sortBy (_._1.name)

  def extractParamValues(pp: PlatronParams): String = paramsToSign(pp) map (_._2) mkString ";"

  def prepareContentForSignature(script: String, pp: PlatronParams, secretKey: String): String =
    s"$script;${extractParamValues(pp)};$secretKey"

  def calcSignature(script: String, pp: PlatronParams, secretKey: String): String =
    HashHelper.md5(prepareContentForSignature(script, pp, secretKey))

  def sign(script: String, pp: PlatronParams, secretKey: String): PlatronParams =
    pp :+ PgSig -> calcSignature(script, pp, secretKey)

  def extractSignature(pp: PlatronParams): Option[String] = pp.toMap.get(PgSig)

  def checkSignature(script: String, pp: PlatronParams, secretKey: String): Boolean =
    extractSignature(pp) == Some(calcSignature(script, pp, secretKey))
}