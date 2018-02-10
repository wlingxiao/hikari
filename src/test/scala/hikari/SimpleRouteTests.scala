package hikari

import hikari.InternalRoute._
import io.netty.channel.embedded.EmbeddedChannel
import io.netty.handler.codec.http.HttpMethod.GET
import io.netty.handler.codec.http.HttpVersion.HTTP_1_1
import io.netty.handler.codec.http.{DefaultFullHttpRequest, FullHttpResponse, HttpObjectAggregator, HttpRequestDecoder}
import io.netty.util.CharsetUtil.UTF_8
import org.scalatest.{FunSuite, Matchers}

class SimpleRouteTests extends FunSuite with Matchers {

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
    before("/users") { (req, resp) =>
      halt(400)
    }

    val request = new DefaultFullHttpRequest(HTTP_1_1, GET, "/users")
    val channel = createChannel()
    channel.writeInbound(request)
    val response = channel.readOutbound[FullHttpResponse]()
    response.status().code() should equal(400)
  }

  private def createChannel(): EmbeddedChannel = {
    new EmbeddedChannel(new HttpRequestDecoder(), new HttpObjectAggregator(Short.MaxValue), new BasicHandler)
  }
}
