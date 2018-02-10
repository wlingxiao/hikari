package hikari

import io.netty.channel.{ChannelHandlerContext, SimpleChannelInboundHandler}
import io.netty.handler.codec.http._
import io.netty.util.AsciiString
import org.slf4j.LoggerFactory

class BasicHandler extends SimpleChannelInboundHandler[FullHttpRequest] {

  private val CONTENT_TYPE = AsciiString.cached("Content-Type")
  private val CONTENT_LENGTH = AsciiString.cached("Content-Length")
  private val CONNECTION = AsciiString.cached("Connection")
  private val KEEP_ALIVE = AsciiString.cached("keep-alive")

  private val log = LoggerFactory.getLogger(this.getClass)

  override def channelReadComplete(ctx: ChannelHandlerContext): Unit = {
    ctx.flush()
  }

  /**
    * 读取 post 和 put body
    * https://stackoverflow.com/questions/34991616/netty-how-to-detect-extract-contents-of-posts
    *
    */
  override def channelRead0(ctx: ChannelHandlerContext, httpRequest: FullHttpRequest): Unit = {
    val request = new Request(httpRequest)
    log.info(s"请求方法为：${request.method}，请求地址为：${request.url}")
    val resp = new Response(ctx)
    ctx.channel().attr(Constants.REQUEST_KEY).setIfAbsent(request)
    ctx.channel().attr(Constants.RESPONSE_KEY).setIfAbsent(resp)
    if (HttpUtil.isKeepAlive(httpRequest)) {
      resp.header(CONNECTION, KEEP_ALIVE)
    }
    findAction(httpRequest.uri(), request, resp)
  }

  override def exceptionCaught(ctx: ChannelHandlerContext, cause: Throwable): Unit = {
    val mapper = new ExceptionHandler
    mapper.runAll(ctx, cause)
  }

  private def findAction(url: String, request: Request, response: Response): Unit = {
    InternalRoute.beforeFilters.find(x => {
      request.pathPattern = x.pathPattern(url)
      request.pathPattern.isDefined
    }).foreach(_.action(request, response))

    val matchedPattern = InternalRoute.getRoutes.find(x => {
      request.pathPattern = x.pathPattern(url)
      request.pathPattern.isDefined
    })

    val body = if (matchedPattern.isDefined) {
      val a = matchedPattern.get
      if (a.method != request.method) {
        InternalRoute.halt(405)
      }
      a.action(request, response)
    }

    InternalRoute.afterMap.find(x => {
      request.pathPattern = x.pathPattern(url)
      request.pathPattern.isDefined
    }).foreach(_.action(request, response))
    response.write(body)
  }
}
