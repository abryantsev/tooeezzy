package com.tooe.core.util

object LegacyPasswordChecker {

  final val salt = "aVQXB1LKB41Oi7sgP5R9gvVUjy1tKfLJu926wDUxSefFAil0ND"

  def check(password: String, hash: String): Boolean = {
    val separatorAt = hash indexOf ":"
    val part1 = hash take separatorAt
    val part2 = hash drop (separatorAt + 1)
    val source = part2 + password + salt
    val calcHash = HashHelper.md5(source)
    part1 == calcHash
  }
}
