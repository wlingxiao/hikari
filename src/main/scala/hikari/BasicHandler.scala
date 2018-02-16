package hikari

import java.net.URI

import io.netty.channel.{ChannelHandlerContext, SimpleChannelInboundHandler}
import io.netty.handler.codec.http._
import io.netty.util.AsciiString
import org.slf4j.LoggerFactory

class BasicHandler extends SimpleChannelInboundHandler[FullHttpRequest] {

  private val CONTENT_TYPE = AsciiString.cached("Content-Type")
  private val CONTENT_LENGTH = AsciiString.cached("Content-Length")
  private val CONNECTION = "Connection"
  private val KEEP_ALIVE = "keep-alive"

  private val log = LoggerFactory.getLogger(this.getClass)

  override def channelReadComplete(ctx: ChannelHandlerContext): Unit = {
    ctx.flush()
  }

  /**
    * 读取 post 和 put body
    * https://stackoverflow.com/questions/34991616/netty-how-to-detect-extract-contents-of-posts
    *
    * io.netty.util.IllegalReferenceCountException: refCnt: 0 in Netty
    * https://stackoverflow.com/questions/41556208/io-netty-util-illegalreferencecountexception-refcnt-0-in-netty
    */
  override def channelRead0(ctx: ChannelHandlerContext, httpRequest: FullHttpRequest): Unit = {
    val request = new Request(httpRequest, ctx)
    log.info(s"请求方法为：${request.method}，请求地址为：${request.path}")
    val resp = new Response(ctx, httpRequest)
    routes.setRequest(request)
    routes.setResponse(resp)
    ctx.channel().attr(Constants.REQUEST_KEY).setIfAbsent(request)
    ctx.channel().attr(Constants.RESPONSE_KEY).setIfAbsent(resp)
    if (HttpUtil.isKeepAlive(httpRequest)) {
      resp.header(CONNECTION, KEEP_ALIVE)
    }
    val uri = new URI(httpRequest.uri())

    findAction(uri.getPath, request, resp)
  }

  private def findAction(url: String, request: Request, response: Response): Unit = {
    InternalRoute.beforeFilters.find(x => {
      request.pathPattern = x.pathPattern(url)
      request.pathPattern.isDefined
    }).foreach(_.action(request, response))

    val pathMatched = InternalRoute.routes.filter(x => {
      x.pathPattern(url).isDefined
    })
    if (pathMatched.isEmpty) {
      InternalRoute.halt(404)
    }

    val methodMatched = pathMatched.filter(x => x.method == request.method)
    if (methodMatched.isEmpty) {
      InternalRoute.halt(405)
    }

    val contentTypedMatched = if (request.method == "POST" || request.method == "PUT") {
      if (request.contentType.isEmpty) {
        InternalRoute.halt(415)
      } else {
        methodMatched.filter(x => {
          if (x.consumes.isEmpty) {
            true
          } else x.consumes.contains(request.contentType.get)
        })
      }
    } else methodMatched

    if (contentTypedMatched.isEmpty) {
      InternalRoute.halt(415)
    }

    val aba = methodMatched.sortWith((x, y) => x > y).headOption
    val a = aba.get
    request.pathPattern = a.pathPattern(url)
    val body = a.action(request, response)

    InternalRoute.afterMap.find(x => {
      request.pathPattern = x.pathPattern(url)
      request.pathPattern.isDefined
    }).foreach(_.action(request, response))
    response.write(body)
  }
}
