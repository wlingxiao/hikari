package hikari

import io.netty.buffer.Unpooled
import io.netty.channel.embedded.EmbeddedChannel
import io.netty.handler.codec.http._
import io.netty.util.CharsetUtil

/**
  * https://searchcode.com/codesearch/view/25262543/
  * https://github.com/zuoyanyouwu/netty-handler-tester
  */
object EmbeddedChannelHttpExample extends App {

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
