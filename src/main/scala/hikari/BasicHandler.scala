package hikari

import java.nio.charset.Charset

import io.netty.buffer.Unpooled
import io.netty.channel.{ChannelFutureListener, ChannelHandlerContext, SimpleChannelInboundHandler}
import io.netty.handler.codec.http.HttpResponseStatus.OK
import io.netty.handler.codec.http.HttpVersion.HTTP_1_1
import io.netty.handler.codec.http._
import io.netty.util.AsciiString

class BasicHandler extends SimpleChannelInboundHandler[FullHttpRequest] {

  private val CONTENT = Array[Byte]('H', 'e', 'l', 'l', 'o', ' ', 'W', 'o', 'r', 'l', 'd')

  private val CONTENT_TYPE = AsciiString.cached("Content-Type")
  private val CONTENT_LENGTH = AsciiString.cached("Content-Length")
  private val CONNECTION = AsciiString.cached("Connection")
  private val KEEP_ALIVE = AsciiString.cached("keep-alive")

  override def channelReadComplete(ctx: ChannelHandlerContext): Unit = {
    ctx.flush()
  }


  /**
    * 读取 post 和 put body
    * https://stackoverflow.com/questions/34991616/netty-how-to-detect-extract-contents-of-posts
    *
    */
  override def channelRead0(ctx: ChannelHandlerContext, httpRequest: FullHttpRequest): Unit = {
    if (httpRequest.method().equals(HttpMethod.POST) || httpRequest.method().equals(HttpMethod.PUT)) {
      println(httpRequest.content().toString(Charset.forName("UTF-8"))) // 读取请求体
    }

    val keepAlive = HttpUtil.isKeepAlive(httpRequest)
    val response = new DefaultFullHttpResponse(HTTP_1_1, OK, Unpooled.wrappedBuffer(CONTENT))

    response.headers().set(CONTENT_TYPE, "text/plain")
    response.headers().setInt(CONTENT_LENGTH, response.content().readableBytes())
    if (!keepAlive) {
      ctx.write(response).addListener(ChannelFutureListener.CLOSE)
    } else {
      response.headers().set(CONNECTION, KEEP_ALIVE)
      ctx.write(response)
    }

  }

  override def exceptionCaught(ctx: ChannelHandlerContext, cause: Throwable): Unit = {
    cause.printStackTrace()
    ctx.close()
  }
}
