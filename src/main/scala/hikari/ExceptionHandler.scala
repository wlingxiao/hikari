package hikari

import io.netty.channel.{ChannelHandlerContext, ChannelInboundHandlerAdapter}
import io.netty.handler.codec.http.HttpVersion.HTTP_1_1
import io.netty.handler.codec.http.{DefaultFullHttpResponse, HttpResponseStatus}
import org.slf4j.LoggerFactory

class ExceptionHandler(mapper: ExceptionMapper*) extends ChannelInboundHandlerAdapter {

  private val log = LoggerFactory.getLogger(this.getClass)

  val defaultMapper: ExceptionMapper = {
    case e: HaltException =>
      new DefaultFullHttpResponse(HTTP_1_1, HttpResponseStatus.valueOf(e.code))
    case e: Exception =>
      log.error("内部错误", e)
      new DefaultFullHttpResponse(HTTP_1_1, HttpResponseStatus.valueOf(500))
    case t: Throwable => throw t
  }

  override def exceptionCaught(ctx: ChannelHandlerContext, cause: Throwable): Unit = {
    val fun = (mapper :+ defaultMapper).reduce((x, y) => x orElse y)
    val resp = ctx.channel().attr(Constants.RESPONSE_KEY).get()
    val body = fun(cause)
    resp.write(body)
  }
}
