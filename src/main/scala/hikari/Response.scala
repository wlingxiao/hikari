package hikari

import java.nio.charset.Charset

import io.netty.buffer.Unpooled
import io.netty.channel.{ChannelFutureListener, ChannelHandlerContext}
import io.netty.handler.codec.http.HttpVersion.HTTP_1_1
import io.netty.handler.codec.http
import io.netty.handler.codec.http.cookie.{DefaultCookie, ServerCookieEncoder, Cookie => NettyCookie}
import io.netty.handler.codec.http.{DefaultFullHttpResponse, HttpResponseStatus, HttpVersion}
import io.netty.util.AsciiString
import scala.collection.JavaConverters._
import scala.collection.mutable
import scala.collection.mutable.ListBuffer
import io.netty.handler.codec.http.HttpHeaderNames._


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
    response.headers().set(SET_COOKIE, ServerCookieEncoder.STRICT.encode(cookieHolder.asJava))
    if (header(KEEP_ALIVE).isDefined) {
      response.headers().set(CONNECTION, KEEP_ALIVE)
      ctx.writeAndFlush(response)
    } else {
      ctx.writeAndFlush(response).addListener(ChannelFutureListener.CLOSE)
    }
  }

  def write(body: Any): Unit = {
    body match {
      case str: String =>
        writeByte(str.getBytes(Charset.forName("UTF-8")))
      case r: DefaultFullHttpResponse =>
        ctx.write(r).addListener(ChannelFutureListener.CLOSE)
      case any: Any => throw new UnsupportedOperationException(s"不支持的响应类型：${any.getClass.getName}")
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

  private val cookieHolder = new ListBuffer[NettyCookie]()

  def cookie(c: Cookie): Unit = {
    cookieHolder += c
  }

  /**
    * Cookie
    */
  def cookie(name: String, value: String): Unit = {
    val c = Cookie(name, value)
    cookieHolder += c
  }

}
