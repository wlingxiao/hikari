package hikari

import hikari.InternalRoute._

object HikariExample extends App {

  before("/users/*") { (request, response) =>

    println("before users")

  }

  get("/users/:id") { (request, response) =>
    request.pathParam("id")
  }

  after("/users/*") { (request, response) =>
    println("after users")
  }

  HikariServer.start()
}
