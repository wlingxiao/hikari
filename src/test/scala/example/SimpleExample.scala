package example

import hikari.Executors._
import hikari.Filters._
import hikari.Routes._
import hikari.{HikariServer, Person}

import scala.concurrent.Future

object SimpleExample extends App {

  before("/users/*") { (request, response) =>

  }

  get("/users/:id") { (request, response) =>

    println(request.cookies())

    println(request.cookie("name"))

    request.pathParam("id")
  }

  post("/users") { (req, _) =>
    val a = req.body[Person]
    println(a)
    "created"
  }

  after("/users/*") { (request, response) =>
    println("after users")
  }

  get("/sessions") { (_, resp) =>
    resp.cookie("token", "helloworld")
    "success"
  }

  get("/jsons") { (_, resp) =>
    Person("test", 100)
  }

  get("/async") { (_, resp) =>
    Future {
      "test"
    }
  }

  HikariServer.start()
}
