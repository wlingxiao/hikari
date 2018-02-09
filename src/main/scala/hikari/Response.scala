package hikari

import java.nio.charset.Charset

import io.netty.buffer.Unpooled
import io.netty.channel.{ChannelFutureListener, ChannelHandlerContext}
import io.netty.handler.codec.http.HttpResponseStatus.OK
import io.netty.handler.codec.http.HttpVersion.HTTP_1_1
import io.netty.handler.codec.http.{DefaultFullHttpResponse, FullHttpRequest, HttpUtil}
import io.netty.util.AsciiString

class Response(ctx: ChannelHandlerContext, httpRequest: FullHttpRequest) {

  private val CONTENT_TYPE = AsciiString.cached("Content-Type")
  private val CONTENT_LENGTH = AsciiString.cached("Content-Length")
  private val CONNECTION = AsciiString.cached("Connection")
  private val KEEP_ALIVE = AsciiString.cached("keep-alive")

  def write(result: Array[Byte]): Unit = {

    val keepAlive = HttpUtil.isKeepAlive(httpRequest)
    val response = new DefaultFullHttpResponse(HTTP_1_1, OK, Unpooled.wrappedBuffer(result))
    response.headers().set(CONTENT_TYPE, "text/plain")
    response.headers().setInt(CONTENT_LENGTH, response.content().readableBytes())
    if (keepAlive) {
      response.headers().set(CONNECTION, KEEP_ALIVE)
      ctx.write(response)
    } else {
      ctx.write(response).addListener(ChannelFutureListener.CLOSE)
    }
  }

  var body: Any = _

  def writeBody(): Unit = {
    body match {
      case str: String =>
        write(str.getBytes(Charset.forName("UTF-8")))
      case r: DefaultFullHttpResponse =>
        ctx.write(r).addListener(ChannelFutureListener.CLOSE)
      case _ => throw new UnsupportedOperationException("不支持的返回类型")
    }

  }

}
