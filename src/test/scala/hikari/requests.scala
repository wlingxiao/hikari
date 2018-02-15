package hikari

import com.fasterxml.jackson.annotation.JsonInclude.Include
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.scala.DefaultScalaModule
import io.netty.buffer.Unpooled
import io.netty.channel.ChannelHandler
import io.netty.channel.embedded.EmbeddedChannel
import io.netty.handler.codec.http.HttpHeaderNames.CONTENT_TYPE
import io.netty.handler.codec.http.HttpMethod.{GET, POST}
import io.netty.handler.codec.http.HttpVersion.HTTP_1_1
import io.netty.handler.codec.http.{DefaultFullHttpRequest, FullHttpResponse, HttpObjectAggregator, HttpRequestDecoder}
import io.netty.util.CharsetUtil.UTF_8

class HttpResponse(resp: FullHttpResponse) {

  def body: String = resp.content().toString(UTF_8)

  def status: Int = resp.status().code()

  def header(name: String): String = resp.headers().get(name)

}

object requests extends ServerConfig {

  private val objectMapper = {
    val ow = new ObjectMapper()
    ow.setSerializationInclusion(Include.NON_NULL)
    ow.registerModule(DefaultScalaModule)
    ow.writer.withDefaultPrettyPrinter
    ow
  }

  def get(path: String, params: Map[String, Any] = Map.empty, headers: Map[String, Any] = Map.empty): HttpResponse = {
    val queryStr = params.map(pair => pair._1 + "=" + pair._2).mkString("&")
    val url = if (params.nonEmpty) {
      path + "?" + queryStr
    } else path
    val request = new DefaultFullHttpRequest(HTTP_1_1, GET, url)
    for ((k, v) <- headers) {
      request.headers().set(k, v)
    }
    val channel = createBasicHandler
    channel.writeInbound(request)
    new HttpResponse(channel.readOutbound[FullHttpResponse]())
  }

  def post(path: String, data: Any = null, params: Map[String, Any] = Map.empty, headers: Map[String, Any] = Map.empty): HttpResponse = {
    val queryStr = params.map(pair => pair._1 + "=" + pair._2).mkString("&")
    val content = if (data != null) {
      objectMapper.writeValueAsString(data).getBytes(UTF_8)
    } else Array.emptyByteArray
    val body = Unpooled.wrappedBuffer(content)

    val url = if (params.nonEmpty) {
      path + "?" + queryStr
    } else path

    val request = new DefaultFullHttpRequest(HTTP_1_1, POST, url, body)
    request.headers().set(CONTENT_TYPE, "application/json")

    for ((k, v) <- headers) {
      request.headers().set(k, v)
    }

    val channel = createBasicHandler
    channel.writeInbound(request)
    new HttpResponse(channel.readOutbound[FullHttpResponse]())
  }

  private def createBasicHandler = createChannel(new DispatchHandler(this))

  private def createChannel(handler: ChannelHandler): EmbeddedChannel = {
    new EmbeddedChannel(new HttpRequestDecoder(), new HttpObjectAggregator(Short.MaxValue), handler)
  }

}
