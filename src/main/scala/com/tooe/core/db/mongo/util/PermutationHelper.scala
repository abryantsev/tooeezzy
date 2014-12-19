package com.tooe.core.db.mongo.util

object PermutationHelper {

  val Name = """[^a-zа-я0-9]*([a-zа-я0-9].*?)[^a-zа-я0-9]*$""".r

  def permuteBySpace(string: String) =
    string.toLowerCase.trim.split(" ").toStream.filter(_.length > 2).take(7).collect {
      case Name(content) => content
    }.permutations.map(_.mkString(" ")).toSeq



}
