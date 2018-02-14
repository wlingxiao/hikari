package hikari

import java.io.{File, RandomAccessFile}
import java.net.URLDecoder
import java.text.SimpleDateFormat
import java.util.Locale

import io.netty.buffer.Unpooled
import io.netty.channel._
import io.netty.handler.codec.http.HttpMethod._
import io.netty.handler.codec.http.HttpResponseStatus._
import io.netty.handler.codec.http.HttpVersion._
import io.netty.handler.codec.http._
import io.netty.util.CharsetUtil
import org.slf4j.LoggerFactory

/**
  * EncoderException: unexpected message type: DefaultFileRegion
  * https://github.com/netty/netty/issues/2466
  */
class StaticFileHandler extends SimpleChannelInboundHandler[FullHttpRequest] {

  private val log = LoggerFactory.getLogger(this.getClass)

  val HTTP_DATE_FORMAT = "EEE, dd MMM yyyy HH:mm:ss zzz"
  val HTTP_DATE_GMT_TIMEZONE = "GMT"
  val HTTP_CACHE_SECONDS = 60

  override def channelRead0(ctx: ChannelHandlerContext, request: FullHttpRequest): Unit = {
    if (!request.decoderResult().isSuccess) {
      sendError(ctx, BAD_REQUEST)
      return
    }

    if (request.method() != GET) {
      sendError(ctx, METHOD_NOT_ALLOWED)
      return
    }

    val uri = request.uri()
    val path = sanitizeUri(uri)
    if (path == null) {
      sendError(ctx, FORBIDDEN)
      return
    }

    val file = new File(path)
    if (file.isHidden || !file.exists()) {
      sendError(ctx, NOT_FOUND)
      return
    }

    val ifModifiedSince = request.headers().get(HttpHeaderNames.IF_MODIFIED_SINCE)
    if (ifModifiedSince != null && !ifModifiedSince.isEmpty) {
      val dateFormatter = new SimpleDateFormat(HTTP_DATE_FORMAT, Locale.US)
      val ifModifiedSinceDate = dateFormatter.parse(ifModifiedSince)
    }

    val raf = new RandomAccessFile(file, "r")
    val fileLength = raf.length()

    val response = new DefaultHttpResponse(HTTP_1_1, OK)
    HttpUtil.setContentLength(response, fileLength)
    setContentTypeHeader(response, file)

    if (HttpUtil.isKeepAlive(request)) {
      response.headers().set(HttpHeaderNames.CONNECTION, HttpHeaderValues.KEEP_ALIVE)
    }
    ctx.write(response)
    val sendFileFuture = ctx.write(new DefaultFileRegion(raf.getChannel, 0, fileLength), ctx.newProgressivePromise())
    sendFileFuture.addListener(new ChannelProgressiveFutureListener() {
      override def operationProgressed(future: ChannelProgressiveFuture, progress: Long, total: Long): Unit = {
        if (total < 0) {
          log.info(future.channel + " Transfer progress: " + progress)
        }
        else log.info(future.channel + " Transfer progress: " + progress + " / " + total)
      }

      override def operationComplete(future: ChannelProgressiveFuture): Unit = {
        if (!future.isSuccess) {
          log.error("传输失败", future.cause())
        } else {
          log.info(future.channel + " Transfer complete.")
        }
      }
    })
    val lastContentFuture = ctx.writeAndFlush(LastHttpContent.EMPTY_LAST_CONTENT)
    if (!HttpUtil.isKeepAlive(request)) { // Close the connection when the whole content is written out.
      lastContentFuture.addListener(ChannelFutureListener.CLOSE)
    }
  }

  private def sanitizeUri(uri: String): String = {
    val decodedUri = URLDecoder.decode(uri, "UTF-8")
    if (decodedUri.isEmpty || uri.charAt(0) != '/') {
      return null
    }

    val basePath = "D:\\projects\\IdeaProjects\\hikari\\dist"
    val filePath = decodedUri.replace("/static", "").replace('/', File.separatorChar)

    basePath + filePath
  }

  private def sendError(ctx: ChannelHandlerContext, status: HttpResponseStatus) = {
    val response = new DefaultFullHttpResponse(HTTP_1_1, status, Unpooled.copiedBuffer("Failure: " + status + "\r\n", CharsetUtil.UTF_8))
    response.headers.set(HttpHeaderNames.CONTENT_TYPE, "text/plain; charset=UTF-8")
    ctx.writeAndFlush(response).addListener(ChannelFutureListener.CLOSE)
  }

  private def setContentTypeHeader(response: HttpResponse, file: File): Unit = {
    val fileName = file.getName
    val contentType = if (fileName.contains("css")) {
      "text/css"
    } else if (fileName.contains("js")) {
      "application/javascript"
    } else {
      "text/html"
    }

    response.headers.set(HttpHeaderNames.CONTENT_TYPE, contentType)
  }
}
