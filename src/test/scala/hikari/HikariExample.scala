package hikari

import hikari.Route._

object HikariExample extends App {

  before("/users/*") { (request, response) =>

    println("before users")

  }

  get("/users/:id") { (request, response) =>

    println("hello world")

  }

  after("/users/*") { (request, response) =>
    println("after users")
  }

  HikariServer.start()
}
