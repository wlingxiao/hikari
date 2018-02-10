package hikari

import hikari.matcher.{PathPattern, SinatraPathPatternParser}

import scala.collection.mutable.ListBuffer

case class RouteEntry(method: String, pathPattern: PathPattern, action: Action)

case class FilterEntry(pathPattern: PathPattern, action: FilterAction)


object InternalRoute {

  val getRoutes = ListBuffer[RouteEntry]()

  private[hikari] val beforeFilters = scala.collection.mutable.ListBuffer[FilterEntry]()

  private[hikari] val afterMap = scala.collection.mutable.ListBuffer[FilterEntry]()

  def get(path: String)(action: Action): Unit = {
    val routeEntry = RouteEntry("GET", SinatraPathPatternParser(path), action)
    getRoutes += routeEntry
  }

  def post(path: String)(action: Action): Unit = {
    val routeEntry = RouteEntry("POST", SinatraPathPatternParser(path), action)
    getRoutes += routeEntry
  }

  def before(path: String)(action: FilterAction): Unit = {
    beforeFilters += FilterEntry(SinatraPathPatternParser(path), action)
  }

  def after(path: String)(action: FilterAction): Unit = {
    afterMap += FilterEntry(SinatraPathPatternParser(path), action)
  }

  def halt(code: Int): Unit = {
    code match {
      case 400 => throw new HaltException(code, "bad request")
      case 405 => throw new HaltException(code, "method not allowed")
      case _ => throw new HaltException(500, "internal server error")
    }
  }

  def clearAll(): Unit = {
    getRoutes.clear()
    beforeFilters.clear()
    afterMap.clear()
  }

}
