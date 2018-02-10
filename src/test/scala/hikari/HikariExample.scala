package hikari

import hikari.InternalRoute._

object HikariExample extends App {

  before("/users/*") { (request, response) =>

    println("before users")

  }

  get("/users/:id") { (request, response) =>

    println(request.cookies())

    println(request.cookie("name"))

    request.pathParam("id")

    throw new UnsupportedOperationException
  }

  after("/users/*") { (request, response) =>
    println("after users")
  }

  HikariServer.start()
}
