package hikari

import io.netty.buffer.Unpooled
import io.netty.channel.embedded.EmbeddedChannel
import io.netty.channel.{ChannelHandlerContext, SimpleChannelInboundHandler}
import io.netty.handler.codec.http._
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

  val channel = new EmbeddedChannel(new HttpRequestDecoder(), new HttpObjectAggregator(Short.MaxValue), new BasicHandler)
  val crlf = "\r\n"

  val request = "GET /some/path HTTP/1.1" + crlf +
    "Host: localhost" + crlf +
    "MyTestHeader: part1" + crlf +
    "              newLinePart2" + crlf +
    "MyTestHeader2: part21" + crlf +
    "\t            newLinePart22" + crlf + crlf
  channel.writeInbound(Unpooled.copiedBuffer(request, CharsetUtil.US_ASCII))
  val req: FullHttpResponse = channel.readOutbound[FullHttpResponse]()
  println(req)
}
