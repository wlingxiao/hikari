package hikari

import hikari.InternalRoute._
import io.netty.channel.embedded.EmbeddedChannel
import io.netty.handler.codec.http.HttpMethod._
import io.netty.handler.codec.http.HttpVersion.HTTP_1_1
import io.netty.handler.codec.http.{DefaultFullHttpRequest, FullHttpResponse, HttpObjectAggregator, HttpRequestDecoder}
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

  private def createChannel(): EmbeddedChannel = {
    new EmbeddedChannel(new HttpRequestDecoder(), new HttpObjectAggregator(Short.MaxValue), new BasicHandler)
  }

  after {
    InternalRoute.clearAll()
  }
}
