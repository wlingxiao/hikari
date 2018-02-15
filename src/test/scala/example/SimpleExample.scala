package example

import java.io.File
import java.nio.file.Paths

import hikari.Executors._
import hikari.Filters._
import hikari.routes._
import hikari.{Binary, ByteBuf, HikariServer}

import scala.concurrent.Future

object SimpleExample extends App {

  before("/users/*") { (request, response) =>

  }

  get("/users/:id") {

    println(request.cookies)

    println(request.cookie("name"))

    request.pathParam("id")
  }

  post("/users") {
    val a = request.body[Person]
    println(a)
    "created"
  }

  after("/users/*") { (request, response) =>
    println("after users")
  }

  get("/sessions") {
    response.cookie("token", "helloworld")
    "success"
  }

  get("/jsons") {
    Person("test", 100)
  }

  get("/async") {
    Future {
      "test"
    }
  }

  post("/files") {
    val f = request.body[ByteBuf]
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

  get("/files") {
    val path = Paths.get("z://work//test")
    Binary(path.toFile, "image/jpeg")
  }

  get("/unit") {}

  get("/query") {
    val a = request.query("age")
    a.get.mkString("")
  }

  post("/form") {
    val ret = request.forms("name")
    ret.get.mkString("")
  }

  val server = new HikariServer {
    override val configFile = "hikari-test"
  }

  server.start()
}
