package hikari

import hikari.matcher.SinatraPathPatternParser

import scala.collection.mutable.ListBuffer
import scala.reflect._

trait Route {

  private[hikari] val routeHolder = new ListBuffer[RouteMeta]()

  protected val prefix = ""

  protected val consumes: List[String] = Nil

  protected val produces: List[String] = Nil

  def get(pattern: String)(any: => Any): Unit = {
    val action = (request: Request, response: Response) => any
    val routeEntry = RouteMeta("GET", SinatraPathPatternParser(pattern), action, pattern)
    routeHolder += routeEntry
  }

  def post(pattern: String)(response: => Any): Unit = {}

}

trait Filter {
  protected val prefix = "/*"

  private[hikari] val routeHolder = new ListBuffer[FilterEntry]()

  def before(filterAction: FilterAction): Unit = {
    val fe = FilterEntry(SinatraPathPatternParser(prefix), filterAction)
    routeHolder += fe
  }

  def after(filterAction: FilterAction): Unit = {
    val fe = FilterEntry(SinatraPathPatternParser(prefix), filterAction)
    routeHolder += fe
  }
}

class UserFilter extends Filter {
  before { (request, response) =>
    println(request.path)
  }
}

class UserRoute extends Route {
  get("/users") {
    "users"
  }

  post("/users") {
    "users"
  }

}

class TokenRoute extends Route {

}

class AppContext {
  def route[R <: Route : ClassTag]: AppContext = {
    val r = classTag[R].runtimeClass.newInstance()
    InternalRoute.routeHolders ++= r.asInstanceOf[Route].routeHolder
    this

  }

  def route(r: Route): AppContext = {
    InternalRoute.routeHolders ++= r.routeHolder
    this
  }

  def filter[F <: Filter : ClassTag]: AppContext = {
    val r = classTag[F].runtimeClass.newInstance()
    InternalRoute.beforeFilters ++= r.asInstanceOf[Filter].routeHolder
    this
  }

  def filter(f: Filter): AppContext = {
    InternalRoute.beforeFilters ++= f.routeHolder
    this
  }
}

object AppContextHolder {

  val appContext = new AppContext

}

trait HttpServer {

  def config(router: AppContext)

  private val _appContext = AppContextHolder.appContext

  def start(): Unit = {
    config(_appContext)
    HikariServer.start()
  }

}

object MyServer extends HttpServer with App {
  def config(context: AppContext): Unit = {
    context.filter(new UserFilter)
    context.route[UserRoute]
    context.route[TokenRoute]
  }

  start()
}
