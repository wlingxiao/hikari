package hikari

import java.io.RandomAccessFile

import io.netty.buffer.{Unpooled, ByteBuf => NettyByteBuf}
import io.netty.channel._
import io.netty.handler.codec.http.HttpHeaderNames._
import io.netty.handler.codec.http.HttpVersion.HTTP_1_1
import io.netty.handler.codec.http.cookie.{ServerCookieEncoder, Cookie => NettyCookie}
import io.netty.handler.codec.http.{DefaultFullHttpResponse, DefaultHttpResponse, FullHttpRequest, FullHttpResponse, HttpHeaderNames, HttpHeaderValues, HttpResponse, HttpResponseStatus, HttpUtil, HttpVersion, LastHttpContent}
import io.netty.util.AsciiString
import org.slf4j.LoggerFactory

import scala.collection.JavaConverters._
import scala.collection.mutable
import scala.collection.mutable.ListBuffer
import scala.concurrent.Future


class Response(var ctx: ChannelHandlerContext, hp: FullHttpRequest) {

  private val log = LoggerFactory.getLogger(this.getClass)

  def write(body: Any): Unit = {
    body match {
      case part: Multipart =>
        writeMultipart(part)
      case future: Future[_] =>
        fireAsyncHandler(future)
      case _: Any =>
        writeNormal(body)
    }
  }

  private def writeNormal(body: Any): Unit = {
    val httpResponse = body match {
      case str: String => new StringTransformer(hp, 200, _contentType).transform(str)
      case dfhp: DefaultFullHttpResponse => dfhp
      case _: Unit => new DefaultFullHttpResponse(privateVersion, HttpResponseStatus.valueOf(status), Unpooled.wrappedBuffer(Array.emptyByteArray))
      case any: Any => new JsonTransformer(200).transform(any)
    }
    putHeader(httpResponse)
    putCookie(httpResponse)
    writeResponse(httpResponse)
  }

  private[this] def putHeader(httpResponse: HttpResponse): Unit = {
    for ((name, value) <- headerMap) httpResponse.headers().set(AsciiString.cached(name), AsciiString.cached(value))
    for ((name, value) <- intHeaderMap) httpResponse.headers().setInt(AsciiString.cached(name), value)
  }

  private[this] def putCookie(httpResponse: HttpResponse): Unit = {
    httpResponse.headers().set(SET_COOKIE, ServerCookieEncoder.STRICT.encode(cookieHolder.asJava))
  }

  private def writeResponse(httpResponse: FullHttpResponse): Unit = {
    if (HttpUtil.isKeepAlive(hp)) {
      httpResponse.headers().set(CONNECTION, HttpHeaderValues.KEEP_ALIVE)
      httpResponse.headers().set(HttpHeaderNames.CONTENT_LENGTH, httpResponse.content().readableBytes())
      ctx.write(httpResponse)
    } else {
      ctx.writeAndFlush(httpResponse).addListener(ChannelFutureListener.CLOSE)
    }
  }

  private def writeMultipart(part: Multipart): Unit = {
    val raf = new RandomAccessFile(part.file, "r")
    val response = new DefaultHttpResponse(HTTP_1_1, HttpResponseStatus.OK)
    response.headers().add(HttpHeaderNames.CONTENT_TYPE, contentType)
    putHeader(response)
    putCookie(response)
    HttpUtil.setTransferEncodingChunked(response, true)
    if (HttpUtil.isKeepAlive(hp)) {
      response.headers.set(HttpHeaderNames.CONNECTION, HttpHeaderValues.KEEP_ALIVE)
    }
    ctx.write(response)
    val region = new DefaultFileRegion(raf.getChannel, 0, raf.length())
    ctx.write(region, ctx.newProgressivePromise())
      .addListener(new ChannelProgressiveFutureListener() {
        override def operationProgressed(future: ChannelProgressiveFuture, progress: Long, total: Long): Unit = {
          if (total < 0) {
            log.error(future.channel + " Transfer progress: " + progress)
          } else {
            log.info(future.channel + " Transfer progress: " + progress + " / " + total)
          }
        }

        override def operationComplete(future: ChannelProgressiveFuture): Unit = {
          log.info(future.channel() + " Transfer complete.")
        }
      })
    val lastContentFuture = ctx.writeAndFlush(LastHttpContent.EMPTY_LAST_CONTENT)
    if (!HttpUtil.isKeepAlive(hp)) {
      lastContentFuture.addListener(ChannelFutureListener.CLOSE)
    }
  }

  private def fireAsyncHandler(future: Future[_]): Unit = {
    ctx.channel().pipeline().addLast(Executors.group, new AsyncHandler(future, this))
    ctx.channel().pipeline().addLast(new ExceptionHandler)
    ctx.fireChannelRead(hp.retain())
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

  private var _contentType = "text/plain"

  def contentType: String = _contentType

  def contentType_(ct: String): Unit = {
    _contentType = ct
  }
}
