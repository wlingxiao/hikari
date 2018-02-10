package hikari

import io.netty.channel.{ChannelHandlerContext, SimpleChannelInboundHandler}
import io.netty.handler.codec.http._

class BasicHandler extends SimpleChannelInboundHandler[FullHttpRequest] {

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
    val resp = new Response(ctx, httpRequest)
    ctx.channel().attr(Constants.REQUEST_KEY).setIfAbsent(request)
    ctx.channel().attr(Constants.RESPONSE_KEY).setIfAbsent(resp)
    findAction(httpRequest.uri(), request, resp)
  }

  override def exceptionCaught(ctx: ChannelHandlerContext, cause: Throwable): Unit = {
    val mapper = new ExceptionHandler
    mapper.runAll(ctx, cause)
  }

  private def findAction(url: String, request: Request, response: Response): Unit = {

    val filterPattern = InternalRoute.beforeFilters.find(x => {
      x._1(url).isDefined
    })

    if (filterPattern.isDefined) {
      val f = filterPattern.get
      f._2(request, response)
    }

    val matchedPattern = InternalRoute.getRoutes.find(x => {
      val a = x.pathPattern(url)
      if (a.isDefined) {
        request.pathPattern = a
        true
      } else false
    })

    val body = if (matchedPattern.isDefined) {
      val a = matchedPattern.get
      if (a.method != request.method) {
        InternalRoute.halt(405)
      }
      a.action(request, response)
    }

    val afterPattern = InternalRoute.afterMap.find(x => {
      x._1(url).isDefined
    })

    if (afterPattern.isDefined) {
      val a = afterPattern.get
      a._2(request, response)
    }

    response.body = body
    response.writeBody()
  }
}
