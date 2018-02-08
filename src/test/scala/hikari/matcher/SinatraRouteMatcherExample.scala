package hikari.matcher

object SinatraRouteMatcherExample extends App {
  val pattern = SinatraPathPatternParser("/*")

  println(pattern("/foo"))
}
