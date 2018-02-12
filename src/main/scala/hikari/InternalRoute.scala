package hikari

import hikari.matcher.{PathPattern, SinatraPathPatternParser}
import io.netty.handler.codec.http.HttpResponseStatus

import scala.collection.mutable.ListBuffer

case class RouteEntry(method: String, pathPattern: PathPattern, action: Action, pattern: String) extends Ordered[RouteEntry] {
  override def compare(that: RouteEntry): Int = {
    if (this.pattern == that.pattern) {
      0
    } else if (this.pattern.contains(":") && that.pattern.contains("*")) {
      1
    } else if (that.pattern.contains(":") && this.pattern.contains("*")) {
      -1
    } else {
      0
    }
  }
}

case class FilterEntry(pathPattern: PathPattern, action: FilterAction)


private[hikari] object InternalRoute {

  private val routeHolders = ListBuffer[RouteEntry]()

  private[hikari] def routes: List[RouteEntry] = routeHolders.toList

  private[hikari] val beforeFilters = scala.collection.mutable.ListBuffer[FilterEntry]()

  private[hikari] val afterMap = scala.collection.mutable.ListBuffer[FilterEntry]()

  def get(path: String)(action: Action): Unit = {
    val routeEntry = RouteEntry("GET", SinatraPathPatternParser(path), action, path)
    routeHolders += routeEntry
  }

  def post(path: String)(action: Action): Unit = {
    val routeEntry = RouteEntry("POST", SinatraPathPatternParser(path), action, path)
    routeHolders += routeEntry
  }

  def put(path: String)(action: Action): Unit = {
    val routeEntry = RouteEntry("PUT", SinatraPathPatternParser(path), action, path)
    routeHolders += routeEntry
  }

  def delete(path: String)(action: Action): Unit = {
    val routeEntry = RouteEntry("DELETE", SinatraPathPatternParser(path), action, path)
    routeHolders += routeEntry
  }

  def options(path: String)(action: Action): Unit = {
    val routeEntry = RouteEntry("OPTIONS", SinatraPathPatternParser(path), action, path)
    routeHolders += routeEntry
  }

  def before(path: String)(action: FilterAction): Unit = {
    beforeFilters += FilterEntry(SinatraPathPatternParser(path), action)
  }

  def after(path: String)(action: FilterAction): Unit = {
    afterMap += FilterEntry(SinatraPathPatternParser(path), action)
  }

  def halt(code: Int): Unit = {
    val status = HttpResponseStatus.valueOf(code)
    throw new HaltException(status.code(), status.reasonPhrase())
  }

  def clearAll(): Unit = {
    routeHolders.clear()
    beforeFilters.clear()
    afterMap.clear()
  }

}
