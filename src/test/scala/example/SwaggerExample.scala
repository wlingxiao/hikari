package example

import java.io.{File, InputStream}
import java.nio.file.{Files, StandardCopyOption}

import hikari.routes._
import hikari.{Binary, HikariServer}

object SwaggerExample extends App {


  get("/swagger/*") {
    val path = request.pathPattern.get.get("splat").get.mkString("/")
    var in: InputStream = null
    val ret: Binary = try {
      val p = "/META-INF/resources/webjars/swagger-ui/2.2.10-1/" + path
      println(p)
      in = getClass.getResourceAsStream(p)
      val file = File.createTempFile(System.currentTimeMillis() + "hello", "temp")
      Files.copy(in, file.toPath, StandardCopyOption.REPLACE_EXISTING)
      new Binary(file, getMimeType(path))
    } finally {
      if (in != null) {
        in.close()
      }
    }
    response.header("Cache-Control", "max-age=360000")
    ret
  }

  def getMimeType(url: String): String = {

    if (url.contains("css")) {
      "text/css"
    } else if (url.contains("js")) {
      "application/javascript"
    } else {
      "text/html"
    }

  }

  HikariServer.start()
}
