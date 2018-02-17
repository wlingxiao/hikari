package hikari

import hikari.matcher.{PathPattern, SinatraPathPatternParser}
import io.netty.handler.codec.http.HttpResponseStatus

import scala.collection.mutable.ListBuffer

case class RouteMeta(method: String, pathPattern: PathPattern, action: Action, pattern: String, consumes: List[String] = Nil) extends Ordered[RouteMeta] {
  override def compare(that: RouteMeta): Int = {
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

  private val routeHolders = ListBuffer[RouteMeta]()

  private[hikari] def routes: List[RouteMeta] = routeHolders.toList

  private[hikari] val beforeFilters = scala.collection.mutable.ListBuffer[FilterEntry]()

  private[hikari] val afterMap = scala.collection.mutable.ListBuffer[FilterEntry]()

  def get(path: String)(any: => Any): Unit = {
    val action = (request: Request, response: Response) => any
    val routeEntry = RouteMeta("GET", SinatraPathPatternParser(path), action, path)
    routeHolders += routeEntry
  }

  def post(path: String, consumes: List[String] = Nil)(any: => Any): Unit = {
    val action = (request: Request, response: Response) => any
    val routeEntry = RouteMeta("POST", SinatraPathPatternParser(path), action, path, consumes)
    routeHolders += routeEntry
  }

  def before(path: String)(action: FilterAction): Unit = {
    beforeFilters += FilterEntry(SinatraPathPatternParser(path), action)
  }

  def after(path: String)(action: FilterAction): Unit = {
    afterMap += FilterEntry(SinatraPathPatternParser(path), action)
  }

  def halt(code: Int): Nothing = {
    val status = HttpResponseStatus.valueOf(code)
    throw new HaltException(status.code(), status.reasonPhrase())
  }

  def clearAll(): Unit = {
    routeHolders.clear()
    beforeFilters.clear()
    afterMap.clear()
  }

}

trait Router {

  protected val consumes: List[String] = Nil

  protected val prefix = ""

  final def request: Request = routes.requestHolder.get()

  final def response: Response = routes.responseHolder.get()

  def get(pattern: String)(resp: => Any): Unit = {
    InternalRoute.get(prefix + pattern)(resp)
  }

  def post(pattern: String)(resp: => Any): Unit = {
    val consu = if (consumes.isEmpty) this.consumes else consumes
    InternalRoute.post(prefix + pattern, consu)(resp)
  }

  def post(pattern: String, consumes: List[String])(resp: => Any): Unit = {
    val consu = if (consumes.isEmpty) this.consumes else consumes
    InternalRoute.post(prefix + pattern, consu)(resp)
  }

}