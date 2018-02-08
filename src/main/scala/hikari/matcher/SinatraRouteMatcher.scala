package hikari.matcher

import hikari.MultiParams

import scala.collection.immutable.Map
import scala.util.matching.Regex
import scala.util.parsing.combinator.RegexParsers

case class PathPattern(regex: Regex, captureGroupNames: List[String] = Nil) {

  def apply(path: String): Option[MultiParams] = {
    // This is a performance hotspot.  Hideous mutatations ahead.
    val m = regex.pattern.matcher(path)
    var multiParams = Map[String, Seq[String]]()
    if (m.matches) {
      var i = 0
      captureGroupNames foreach { name =>
        i += 1
        val value = m.group(i)
        if (value != null) {
          val values = multiParams.getOrElse(name, Vector()) :+ value
          multiParams = multiParams.updated(name, values)
        }
      }
      Some(multiParams)
    } else None
  }

  def +(pathPattern: PathPattern): PathPattern = PathPattern(
    new Regex(this.regex.toString + pathPattern.regex.toString),
    this.captureGroupNames ::: pathPattern.captureGroupNames
  )

}

trait PathPatternParser {

  def apply(pattern: String): PathPattern

}

object MultiMap {

  def apply(): MultiMap = new MultiMap

  def apply[SeqType <: Seq[String]](wrapped: Map[String, SeqType]): MultiMap = new MultiMap(wrapped)

  def empty: MultiMap = apply()

  implicit def map2MultiMap(map: Map[String, Seq[String]]): MultiMap = new MultiMap(map)

}

class MultiMap(wrapped: Map[String, Seq[String]] = Map.empty)
  extends Map[String, Seq[String]] {

  def get(key: String): Option[Seq[String]] = {
    if (key.endsWith("[]")) {
      wrapped.get(key)
    } else {
      wrapped.get(key) orElse wrapped.get(key + "[]")
    }
  }

  def get(key: Symbol): Option[Seq[String]] = get(key.name)

  def +[B1 >: Seq[String]](kv: (String, B1)): MultiMap = {
    new MultiMap(wrapped + kv.asInstanceOf[(String, Seq[String])])
  }

  def -(key: String): MultiMap = new MultiMap(wrapped - key)

  def iterator: Iterator[(String, Seq[String])] = wrapped.iterator

  override def default(a: String): Seq[String] = wrapped.default(a)

}

class SinatraPathPatternParser extends RegexPathPatternParser {

  def apply(pattern: String): PathPattern =
    parseAll(pathPattern, pattern) match {
      case Success(pathPattern, _) =>
        (PartialPathPattern("^") + pathPattern + PartialPathPattern("$")).toPathPattern
      case _ =>
        throw new IllegalArgumentException("Invalid path pattern: " + pattern)
    }

  private def pathPattern = rep(token) ^^ {
    _.reduceLeft {
      _ + _
    }
  }

  private def token = splat | namedGroup | literal

  private def splat = "*" ^^^ PartialPathPattern("(.*?)", List("splat"))

  private def namedGroup = ":" ~> """\w+""".r ^^ { groupName => PartialPathPattern("([^/?#]+)", List(groupName)) }

  private def literal = metaChar | normalChar

  private def metaChar =
    """[\.\+\(\)\$]""".r ^^ { c => PartialPathPattern("\\" + c) }

  private def normalChar = ".".r ^^ { c => PartialPathPattern(c) }

}

object SinatraPathPatternParser {

  def apply(pattern: String): PathPattern = new SinatraPathPatternParser().apply(pattern)

}


trait RegexPathPatternParser extends PathPatternParser with RegexParsers {

  /**
    * This parser gradually builds a regular expression.  Some intermediate
    * strings are not valid regexes, so we wait to compile until the end.
    */
  protected case class PartialPathPattern(regex: String, captureGroupNames: List[String] = Nil) {

    def toPathPattern: PathPattern = PathPattern(regex.r, captureGroupNames)

    def +(other: PartialPathPattern): PartialPathPattern = PartialPathPattern(
      this.regex + other.regex,
      this.captureGroupNames ::: other.captureGroupNames
    )
  }

}