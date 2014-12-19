package com.tooe.core.util

import java.util.UUID
import java.security.MessageDigest

object HashHelper {
  
  def uuid: String = UUID.randomUUID().toString

  def str(prefix: String): String = prefix + ":" + uuid

  def sha1(in: String): String= calcHash(in, md = MessageDigest.getInstance("SHA-1"))

  def md5(in: String): String = calcHash(in, md = MessageDigest.getInstance("MD5"))

  private def calcHash(input: String, md: MessageDigest) = hashToString(md.digest(input.getBytes("UTF-8")))
  
  private def hashToString(hash: Array[Byte])= {
    val sb = new StringBuffer()
    for (i <- 0 until hash.length) {
      sb.append(Integer.toHexString((hash(i) & 0xFF) | 0x100).substring(1,3))
    }
    sb.toString
  }

  def passwordHash(password: String) = sha1(password)
}