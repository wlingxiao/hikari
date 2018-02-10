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

class SimpleRouteTests extends FunSuite with Matchers with BeforeAndAfter {

  test("get users") {
    get("/users") { (_, _) =>
      "users"
    }

    val request = new DefaultFullHttpRequest(HTTP_1_1, GET, "/users")
    val channel = createChannel()
    channel.writeInbound(request)
    val response = channel.readOutbound[FullHttpResponse]()
    response.content().toString(UTF_8) should equal("users")
  }

  test("before get users") {
    InternalRoute.before("/users") { (req, resp) =>
      halt(400)
    }

    val request = new DefaultFullHttpRequest(HTTP_1_1, GET, "/users")
    val channel = createChannel()
    channel.writeInbound(request)
    val response = channel.readOutbound[FullHttpResponse]()
    response.status().code() should equal(400)
  }

  test("post users") {
    post("/users") { (_, _) =>
      "created"
    }

    val request = new DefaultFullHttpRequest(HTTP_1_1, POST, "/users")
    val channel = createChannel()
    channel.writeInbound(request)
    val response = channel.readOutbound[FullHttpResponse]()
    response.content().toString(UTF_8) should equal("created")
  }

  test("post users body") {
    post("/users") { (req, _) =>
      req.body.getOrElse("empty")
    }

    val body = Unpooled.wrappedBuffer("""{"admin": "test"}""".getBytes(UTF_8))
    val request = new DefaultFullHttpRequest(HTTP_1_1, POST, "/users", body)
    val channel = createChannel()
    channel.writeInbound(request)
    val response = channel.readOutbound[FullHttpResponse]()
    response.content().toString(UTF_8) should include("admin")
  }

  test("post users header") {
    post("/users") { (req, _) =>
      req.header(CONTENT_TYPE.toString).getOrElse("empty")
    }

    val body = Unpooled.wrappedBuffer("""{"admin": "test"}""".getBytes(UTF_8))
    val request = new DefaultFullHttpRequest(HTTP_1_1, POST, "/users", body)
    HttpHeaders.setHeader(request, CONTENT_TYPE, "application/json")


    val channel = createChannel()
    channel.writeInbound(request)
    val response = channel.readOutbound[FullHttpResponse]()
    response.content().toString(UTF_8) should equal("application/json")
  }

  private def createChannel(): EmbeddedChannel = {
    new EmbeddedChannel(new HttpRequestDecoder(), new HttpObjectAggregator(Short.MaxValue), new BasicHandler)
  }

  after {
    InternalRoute.clearAll()
  }
}
