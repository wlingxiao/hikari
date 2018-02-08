package hikari

import io.netty.buffer.Unpooled
import io.netty.channel.{ChannelFutureListener, ChannelHandlerContext, ChannelInboundHandlerAdapter}
import io.netty.handler.codec.http.HttpResponseStatus.OK
import io.netty.handler.codec.http.HttpVersion.HTTP_1_1
import io.netty.handler.codec.http.{DefaultFullHttpResponse, HttpRequest, HttpUtil}
import io.netty.util.AsciiString

class BasicHandler extends ChannelInboundHandlerAdapter {

  private val CONTENT = Array[Byte]('H', 'e', 'l', 'l', 'o', ' ', 'W', 'o', 'r', 'l', 'd')

  private val CONTENT_TYPE = AsciiString.cached("Content-Type")
  private val CONTENT_LENGTH = AsciiString.cached("Content-Length")
  private val CONNECTION = AsciiString.cached("Connection")
  private val KEEP_ALIVE = AsciiString.cached("keep-alive")

  override def channelReadComplete(ctx: ChannelHandlerContext): Unit = {
    ctx.flush()
  }


  override def channelRead(ctx: ChannelHandlerContext, msg: Any): Unit = {
    msg match {
      case httpRequest: HttpRequest =>
        println("Hello world")
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
      case _ => doNothing(msg)
    }
  }

  private def doNothing(msg: Any): Unit = {

  }

  override def exceptionCaught(ctx: ChannelHandlerContext, cause: Throwable): Unit = {
    cause.printStackTrace()
    ctx.close()
  }
}
