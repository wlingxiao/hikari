package hikari

import java.nio.charset.Charset

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.scala.DefaultScalaModule
import io.netty.buffer.{Unpooled, ByteBuf => NettyByteBuf}
import io.netty.channel.{ChannelFutureListener, ChannelHandlerContext}
import io.netty.handler.codec.http.HttpHeaderNames._
import io.netty.handler.codec.http.HttpVersion.HTTP_1_1
import io.netty.handler.codec.http.cookie.{ServerCookieEncoder, Cookie => NettyCookie}
import io.netty.handler.codec.http.{DefaultFullHttpResponse, FullHttpRequest, HttpResponseStatus, HttpVersion}
import io.netty.util.AsciiString

import scala.collection.JavaConverters._
import scala.collection.mutable
import scala.collection.mutable.ListBuffer
import scala.concurrent.Future


class Response(ctx: ChannelHandlerContext, hp: FullHttpRequest) {

  private val CONTENT_TYPE = "Content-Type"
  private val CONTENT_LENGTH = "Content-Length"
  private val CONNECTION = AsciiString.cached("Connection")
  private val KEEP_ALIVE = AsciiString.cached("keep-alive")

  private def writeByte(result: Array[Byte], contentType: String = "text/plain"): Unit = {
    val response = new DefaultFullHttpResponse(privateVersion, HttpResponseStatus.valueOf(status), Unpooled.wrappedBuffer(result))
    header(CONTENT_TYPE, contentType)
    header(CONTENT_LENGTH, response.content().readableBytes())
    for ((name, value) <- headerMap) {
      response.headers().set(AsciiString.cached(name), AsciiString.cached(value))
    }
    for ((name, value) <- intHeaderMap) {
      response.headers().setInt(AsciiString.cached(name), value)
    }
    response.headers().set(SET_COOKIE, ServerCookieEncoder.STRICT.encode(cookieHolder.asJava))
    if (header("keep-alive").isDefined) {
      response.headers().set(CONNECTION, KEEP_ALIVE)
      ctx.writeAndFlush(response)
    } else {
      ctx.writeAndFlush(response).addListener(ChannelFutureListener.CLOSE)
    }
  }

  private def writeByteBuf(result: NettyByteBuf, contentType: String = "text/plain"): Unit = {
    val response = new DefaultFullHttpResponse(privateVersion, HttpResponseStatus.valueOf(status), result)
    header(CONTENT_TYPE, contentType)
    header(CONTENT_LENGTH, response.content().readableBytes())
    for ((name, value) <- headerMap) {
      response.headers().set(AsciiString.cached(name), AsciiString.cached(value))
    }
    for ((name, value) <- intHeaderMap) {
      response.headers().setInt(AsciiString.cached(name), value)
    }
    response.headers().set(SET_COOKIE, ServerCookieEncoder.STRICT.encode(cookieHolder.asJava))
    if (header("keep-alive").isDefined) {
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
      case bytes: ByteBuf =>
        writeByteBuf(bytes.buffer, bytes.contentType)
      case r: DefaultFullHttpResponse =>
        ctx.write(r).addListener(ChannelFutureListener.CLOSE)
      case f: Future[_] =>
        ctx.channel().pipeline().addLast(Executors.group, new AsyncHandler(f))
        ctx.fireChannelRead(hp.retain())
      case any: Any =>
        val ow = new ObjectMapper()
        ow.registerModule(DefaultScalaModule)
        ow.writer.withDefaultPrettyPrinter
        val json = ow.writeValueAsString(any)
        writeByte(json.getBytes(Charset.forName("UTF-8")), "application/json; charset=UTF-8")
    }
  }

  def header(name: String, value: String): Unit = {
    headerMap(name) = value
  }

  def header(name: String, value: Int): Unit = {
    intHeaderMap(name) = value
  }

  def header(name: String): Option[String] = {
    headerMap.get(name).orElse(intHeaderMap.get(name)).map(_.toString)
  }

  private val headerMap = mutable.HashMap[String, String]()

  private val intHeaderMap = mutable.HashMap[String, Int]()

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
