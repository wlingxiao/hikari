package example

import org.json4s.{DefaultFormats, Formats}

case class Person(name: String, age: Int)

/**
  * https://stackoverflow.com/questions/16277346/how-to-get-manifest-in-the-pattern-matching
  *
  */
object ManifestExample extends App {

  private implicit val formats: Formats = DefaultFormats

  def parseStr[T](str: String)(implicit mf: Manifest[T]): Unit = {
    mf match {
      case m if m == manifest[Int] => println("Hello world") // 这个地方应该用 manifest so 上的答案有问题
      case _ => println("hh")
    }
  }

  val personStr = """{"name": "test", "age": 10}"""

  val ret = parseStr[Int](personStr)

  println(ret)
}
