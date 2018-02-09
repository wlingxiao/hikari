package hikari

import io.netty.buffer.Unpooled
import io.netty.channel.embedded.EmbeddedChannel
import io.netty.handler.codec.http._
import io.netty.util.CharsetUtil

/**
  * https://searchcode.com/codesearch/view/25262543/
  */
object EmbeddedChannelHttpExample {

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
