package example

import java.io.File
import java.nio.file.Paths

import hikari.Executors._
import hikari.Filters._
import hikari.Routes._
import hikari.{Binary, ByteBuf, HikariServer}

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

  post("/files") { (req, _) =>
    val f = req.body[ByteBuf]
    if (f.isDefined) {
      val buffer = f.get.buffer
      val file = new File("Z:\\Work" + "\\test")

      import java.io.FileOutputStream
      val outputStream = new FileOutputStream(file)
      val size = buffer.readableBytes()
      try {
        val localfileChannel = outputStream.getChannel
        val byteBuffer = buffer.nioBuffer
        var written = 0
        while (written < size) {
          written += localfileChannel.write(byteBuffer)
        }
        buffer.readerIndex(buffer.readerIndex + written)
        localfileChannel.force(false)
      } finally outputStream.close()
    }


    "success"
  }

  get("/files") { (_, _) =>
    val path = Paths.get("z://work//test")
    Binary(path.toFile, "image/jpeg")
  }

  get("/unit") { (_, _) => }

  get("/query") { (req, _) =>
    val a = req.query("age")
    a.get.mkString("")
  }

  HikariServer.start()
}
