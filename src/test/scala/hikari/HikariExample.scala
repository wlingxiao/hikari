package hikari

import hikari.InternalRoute._

object HikariExample extends App {

  before("/users/*") { (request, response) =>

    halt(400)

  }

  get("/users/:id") { (request, response) =>

    println(request.cookies())

    println(request.cookie("name"))

    request.pathParam("id")

    throw new UnsupportedOperationException
  }

  post("/users") { (request, response) =>
    "created"
  }

  after("/users/*") { (request, response) =>
    println("after users")
  }

  HikariServer.start()
}
