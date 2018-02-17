package example

import hikari.{AppContext, Filter, HttpServer, Route}

class UserRoute extends Route {

  get("/users") {

  }

}

class UserFilter extends Filter {
  override val prefix = "/users*"

  before { (request, response) =>
    println(request.path)
  }

}

object NewStyleExample extends HttpServer with App {
  def config(router: AppContext): Unit = {
    router.route[UserRoute]
    router.filter[UserFilter]
  }

  start()
}
