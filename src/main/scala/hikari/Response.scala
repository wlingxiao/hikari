package hikari

import java.nio.charset.Charset

import io.netty.buffer.Unpooled
import io.netty.channel.{ChannelFutureListener, ChannelHandlerContext}
import io.netty.handler.codec.http.HttpVersion.HTTP_1_1
import io.netty.handler.codec.http._
import io.netty.util.AsciiString

import scala.collection.mutable

class Response(ctx: ChannelHandlerContext) {

  private val CONTENT_TYPE = AsciiString.cached("Content-Type")
  private val CONTENT_LENGTH = AsciiString.cached("Content-Length")
  private val CONNECTION = AsciiString.cached("Connection")
  private val KEEP_ALIVE = AsciiString.cached("keep-alive")

  private def writeByte(result: Array[Byte], contentType: CharSequence = "text/plain"): Unit = {
    val response = new DefaultFullHttpResponse(privateVersion, HttpResponseStatus.valueOf(status), Unpooled.wrappedBuffer(result))
    header(CONTENT_TYPE, contentType)
    header(CONTENT_LENGTH, response.content().readableBytes())
    for ((name, value) <- headerMap) {
      response.headers().set(name, value)
    }
    for ((name, value) <- intHeaderMap) {
      response.headers().setInt(name, value)
    }
    if (header(KEEP_ALIVE).isDefined) {
      response.headers().set(CONNECTION, KEEP_ALIVE)
      ctx.write(response)
    } else {
      ctx.write(response).addListener(ChannelFutureListener.CLOSE)
    }
  }

  def write(body: Any): Unit = {
    body match {
      case str: String =>
        writeByte(str.getBytes(Charset.forName("UTF-8")))
      case r: DefaultFullHttpResponse =>
        ctx.write(r).addListener(ChannelFutureListener.CLOSE)
      case _ => throw new UnsupportedOperationException("不支持的返回类型")
    }
  }

  def header(name: CharSequence, value: CharSequence): Unit = {
    headerMap(name) = value
  }

  def header(name: CharSequence, value: Int): Unit = {
    intHeaderMap(name) = value
  }

  def header(name: CharSequence): Option[CharSequence] = {
    headerMap.get(name).orElse(intHeaderMap.get(name)).map(_.toString)
  }

  private val headerMap = mutable.HashMap[CharSequence, CharSequence]()

  private val intHeaderMap = mutable.HashMap[CharSequence, Int]()

  /**
    * http status
    */
  private var privateStatus = 200

  def status: Int = privateStatus

  def status_(newValue: Int): Unit = {
    privateStatus = newValue
  }

  /**
    * http version
    */
  private var privateVersion = HTTP_1_1

  def version: String = privateVersion.protocolName()

  def version_(newValue: String): Unit = {
    privateVersion = HttpVersion.valueOf(newValue)
  }

}
