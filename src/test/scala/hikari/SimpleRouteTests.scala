package hikari

import hikari.InternalRoute._
import io.netty.buffer.Unpooled
import io.netty.channel.embedded.EmbeddedChannel
import io.netty.handler.codec.http.HttpHeaderNames.CONTENT_TYPE
import io.netty.handler.codec.http.HttpMethod._
import io.netty.handler.codec.http.HttpVersion.HTTP_1_1
import io.netty.handler.codec.http._
import io.netty.util.CharsetUtil.UTF_8
import org.scalatest.{BeforeAndAfter, FunSuite, Matchers}

case class TestUser(admin: String)

class SimpleRouteTests extends FunSuite with Matchers with BeforeAndAfter {

  test("get users") {
    get("/users") { (_, _) =>
      "users"
    }
    requests.get("/users").body should equal("users")
  }

  test("before get users") {
    InternalRoute.before("/users") { (req, resp) =>
      halt(400)
    }

    requests.get("/users").status should equal(400)
  }

  test("post users") {
    post("/users") { (_, _) =>
      "created"
    }
    requests.post("/users").body should equal("created")
  }

  test("post users body") {
    post("/users") { (req, _) =>
      val r = req.body[TestUser]
      r.get.toString
    }
    requests.post("/users", Map("admin" -> "test")).body should include("test")
  }

  test("post users header") {
    post("/users") { (req, _) =>
      req.header(CONTENT_TYPE.toString).getOrElse("empty")
    }
    requests.post("/users",
      Map("admin" -> "test"),
      headers = Map("content-type" -> "application/json")).body should equal("application/json")
  }

  test("get users with id and wildcard") {
    get("/users/*") { (req, _) =>
      "wildcard"
    }

    get("/users/:id") { (req, _) =>
      req.pathParam("id")
    }
    requests.get("/users/123").body should equal("123")
  }

  test("在 Request 中共享参数") {

    InternalRoute.before("/users/*") { (req, _) =>
      req.attr[String]("626", "test")
    }

    get("/users/:id") { (req, _) =>
      req.attr[String]("626").get
    }

    val request = new DefaultFullHttpRequest(HTTP_1_1, GET, "/users/123")
    val channel = createChannel()
    channel.writeInbound(request)
    val response = channel.readOutbound[FullHttpResponse]()
    response.content().toString(UTF_8) should equal("test")
  }

  test("async get") {
    get("/async") { (_, _) =>
      "async"
    }

    val request = new DefaultFullHttpRequest(HTTP_1_1, GET, "/async")
    val channel = createChannel()
    channel.writeInbound(request)
    val response = channel.readOutbound[FullHttpResponse]()
    response.content().toString(UTF_8) should equal("async")
  }

  test("full routes") {
    get("/users/:id") { (req, _) =>
      "get"
    }

    post("/users/:id") { (req, _) =>
      "post"
    }

    val request = new DefaultFullHttpRequest(HTTP_1_1, POST, "/users/123")
    val channel = createChannel()
    channel.writeInbound(request)
    val response = channel.readOutbound[FullHttpResponse]()
    response.content().toString(UTF_8) should equal("post")
  }

  test("get with put") {
    get("/users/:id") { (req, _) =>
      "get"
    }

    put("/users/:id") { (req, _) =>
      "put"
    }

    val request = new DefaultFullHttpRequest(HTTP_1_1, PUT, "/users/123")
    val channel = createChannel()
    channel.writeInbound(request)
    val response = channel.readOutbound[FullHttpResponse]()
    response.content().toString(UTF_8) should equal("put")
  }

  /*test("download file") {
    get("/files") { (req, _) =>
      val bytes = Array[Byte]('a', 'b', 'c')
      ByteBuf(bytes, "text/plain")
    }

    val request = new DefaultFullHttpRequest(HTTP_1_1, GET, "/files")
    val channel = createChannel()
    channel.writeInbound(request)
    val response = channel.readOutbound[FullHttpResponse]()
    response.content().toString(UTF_8) should equal("abc")
  }*/

  test("upload file") {
    post("/files") { (req, _) =>
      val buf = req.body[ByteBuf]
      buf.get.contentType
    }

    val body = Unpooled.wrappedBuffer("abc".getBytes(UTF_8))
    val request: DefaultFullHttpRequest = new DefaultFullHttpRequest(HTTP_1_1, POST, "/files", body)
    request.headers().set(CONTENT_TYPE, "image/jpeg")

    val channel = createChannel()
    channel.writeInbound(request)
    val response = channel.readOutbound[FullHttpResponse]()
    response.content().toString(UTF_8) should equal("image/jpeg")
  }

  test("empty response") {
    get("/unit") { (_, _) => }

    val request = new DefaultFullHttpRequest(HTTP_1_1, GET, "/unit")
    val channel = createChannel()
    channel.writeInbound(request)
    val response = channel.readOutbound[FullHttpResponse]()
    response.content().toString(UTF_8) should equal("")
  }

  test("获取单个查询参数") {

    get("/query") { (req, _) =>
      req.query("name").get.mkString("")
    }

    val request = new DefaultFullHttpRequest(HTTP_1_1, GET, "/query?name=query")
    val channel = createChannel()
    channel.writeInbound(request)
    val response = channel.readOutbound[FullHttpResponse]()
    response.content().toString(UTF_8) should equal("query")

  }

  private def createChannel(): EmbeddedChannel = {
    new EmbeddedChannel(new HttpRequestDecoder(), new HttpObjectAggregator(Short.MaxValue), new BasicHandler)
  }

  after {
    InternalRoute.clearAll()
  }
}
