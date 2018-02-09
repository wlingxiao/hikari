package hikari

import java.nio.charset.{Charset, StandardCharsets}

import example.HttpHelloWorldServerHandler
import hikari.InternalRoute._
import io.netty.buffer.{ByteBuf, Unpooled}
import io.netty.channel.{ChannelHandlerContext, SimpleChannelInboundHandler}
import io.netty.channel.embedded.EmbeddedChannel
import io.netty.handler.codec.http._
import io.netty.handler.codec.string.StringDecoder
import io.netty.handler.codec.{DelimiterBasedFrameDecoder, Delimiters}
import io.netty.util.CharsetUtil

class HelloWorldHandler extends SimpleChannelInboundHandler[FullHttpRequest] {

  override def channelReadComplete(ctx: ChannelHandlerContext): Unit = {
    ctx.flush()
  }

  override def channelRead0(ctx: ChannelHandlerContext, httpRequest: FullHttpRequest): Unit = {
    println("hello world")
  }


}

object HikariExample extends App {

  val channel = new EmbeddedChannel(new HttpRequestDecoder())
  val p = channel.pipeline()
  p.addLast(new HttpServerCodec())
  p.addLast(new HttpObjectAggregator(Short.MaxValue)) // 必不可少
  p.addLast(new HttpServerExpectContinueHandler())
  p.addLast(new BasicHandler)
  val crlf = "\r\n"

  val request = "GET /some/path HTTP/1.1" + crlf +
    "Host: localhost" + crlf +
    "MyTestHeader: part1" + crlf +
    "              newLinePart2" + crlf +
    "MyTestHeader2: part21" + crlf +
    "\t            newLinePart22" + crlf + crlf
  channel.writeInbound(Unpooled.copiedBuffer(request, CharsetUtil.US_ASCII))
  val req: HttpRequest = channel.readInbound()

}
