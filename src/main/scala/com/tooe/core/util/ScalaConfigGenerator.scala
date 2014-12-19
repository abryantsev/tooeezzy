package com.tooe.core.util

import java.io.{FileWriter, File}
import scala.collection.mutable.ListBuffer
import scala.io.Source
import scala.util.Try

object ScalaConfigGenerator extends App {
  val config = Source.fromFile(new File("./config/tooe_images.conf")).getLines().withFilter(!_.isEmpty).map {
    str =>
      val (first, second) = str.span(_ != '=')
      first.takeWhile(_ != '#') + (if (second.isEmpty) "" else second.head)
  }.map(_.filterNot(_.isWhitespace)).mkString("\n")

  trait Figure {
    def isWord = false
    def isDelimiter = true
  }
  case object `{` extends Figure
  case object `}` extends Figure
  case object `.` extends Figure
  case object `=` extends Figure
  case object NewLine extends Figure
  case class Word(str: String) extends Figure {
    override def toString =
      Try[String] {
        str.head.toUpper + str.toLowerCase.tail
      }.getOrElse("")
    override def isWord = true
    override def isDelimiter = false
  }

  implicit class SeqOps[A](val xs: Vector[A]) extends AnyVal {
    def dropWhileRight(p: A => Boolean): Vector[A] = {
      xs.reverse.dropWhile(p).reverse.toVector
    }
    def dropWhileRightIncl(p: A => Boolean): Vector[A] = {
      xs.reverse.dropWhile(p).tail.reverse.toVector
    }
  }

  object Figure {
    def parse(str: String): Vector[Figure] = {
      var word: String = ""
      var string: Vector[Figure] = Vector.empty

      def makeWord = {
        val w = Word(word)
        word = ""
        w
      }

      str.foreach {
        case '.' =>
          string = string :+ makeWord :+ `.`
        case '{' =>
          string = string :+ makeWord :+ `{`
        case '=' =>
          string = string :+ makeWord :+ `=`
        case '}' =>
          if (!word.isEmpty) {
            string = string :+ makeWord :+ `}`
          } else
            string = string :+ `}`
        case '\n' =>
          string = string :+ makeWord
        case a if a.isLetter || a == '_' =>
          word = word + a
      }
      string.filterNot(_ == Word("")).+:(`{`).:+(`}`)
    }
  }

  class Current() {
    val bulb = new ListBuffer[Vector[Figure]]()
    var current: Vector[Figure] = Vector.empty

    def magic(xs: Seq[Figure]): List[Vector[Figure]] = {
      xs.foreach {
        case w: Word =>
          current = current :+ w
        case `}` =>
          current = current.dropWhileRightIncl(_ != `{`).dropWhileRight(_ != `{`)
        case `=` =>
          bulb += current.filter(_.isWord)
          current = current.dropWhileRight(_ != `{`)
        case any: Figure =>
          current = current :+ any
      }
      bulb.toList
    }
  }

  def liftMap(mapka: Map[Vector[Figure], String]): Map[Figure, Map[Vector[Figure], String]] = {
    mapka.groupBy(_._1.head).mapValues {
      case map => map.map {
        case (k, v) => (k.tail, v)
      }
    }
  }

  def render(mp: Map[Figure, Map[Vector[Figure], String]]): Vector[String] = {
    mp.map {
      case (k, m) =>
        val (toLift, toInline) = m.partition(_._1.length > 1)
        val inlined: String = toInline.map(xs => s"\nval ${xs._1.head} = config.getString(" + '"' + xs._2 + '"' + ")").mkString
        s"\nobject $k {$inlined ${render(liftMap(toLift)).mkString}}"
    }.toVector
  }

  val parsed = Figure.parse(config)
  val combined = new Current().magic(parsed)

  val po = "package com.tooe.core\n" +
    "import com.tooe.core.main.SharedActorSystem._\n" +
    "\npackage object util {\n" +
    "val config = defaultAkkaConfig\n"

  val writer = new FileWriter(new File("src/main/scala/com/tooe/core/util/package.scala"))

  writer.write(po)

  render(combined.map {
    xs => (xs, xs.map(w => w.asInstanceOf[Word].str).mkString("."))
  }.toMap.groupBy(_._1.head).mapValues {
    case map => map.map {
      case (k, v) => (k.tail, v)
    }
  }).withFilter(!_.isEmpty).foreach {
    str =>
      writer.write(str)
  }

  writer.write("}")

  writer.close()
}