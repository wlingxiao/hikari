package hikari

import io.netty.channel.ChannelHandlerContext
import io.netty.handler.codec.http.HttpVersion.HTTP_1_1
import io.netty.handler.codec.http.{DefaultFullHttpResponse, HttpResponseStatus}
import org.slf4j.LoggerFactory

import scala.collection.mutable.ListBuffer

class ExceptionHandler {

  private val log = LoggerFactory.getLogger(this.getClass)

  private lazy val exceptionMappers = {

    val haltExceptionMapper: ExceptionMapper = {
      case e: HaltException =>
        new DefaultFullHttpResponse(HTTP_1_1, HttpResponseStatus.valueOf(e.code))
      case t: Throwable =>
        log.error("内部错误", t)
        new DefaultFullHttpResponse(HTTP_1_1, HttpResponseStatus.valueOf(500))
    }

    val m = ListBuffer[ExceptionMapper]()
    m += haltExceptionMapper
  }

  def register(em: ExceptionMapper): Unit = {
    exceptionMappers += em
  }

  private[hikari] def runAll(ctx: ChannelHandlerContext, cause: Throwable): Unit = {
    val fun = exceptionMappers.reduce((x, y) => {
      x orElse y
    })
    val resp = ctx.channel().attr(Constants.RESPONSE_KEY).get()
    val body = fun(cause)
    resp.write(body)
  }

}
