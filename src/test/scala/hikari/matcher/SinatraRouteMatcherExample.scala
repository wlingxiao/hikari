package hikari.matcher

object SinatraRouteMatcherExample extends App {
  val pattern = SinatraPathPatternParser("/foo/:bar")

  println(pattern("/foo/123").get("bar"))
}
