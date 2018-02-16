package hikari

import hikari.Executors._
import io.netty.channel.{ChannelHandlerContext, SimpleChannelInboundHandler}
import io.netty.handler.codec.http.FullHttpRequest
import org.slf4j.LoggerFactory

import scala.concurrent.Future
import scala.util.{Failure, Success}

class AsyncHandler(future: Future[_], response: Response) extends SimpleChannelInboundHandler[FullHttpRequest] {

  private val log = LoggerFactory.getLogger(this.getClass)

  override def channelRead0(ctx: ChannelHandlerContext, msg: FullHttpRequest): Unit = {
    future.onComplete {
      case Success(s) =>
        s match {
          case _: Future[_] => log.error("异步响应的实体不能为 Future[_]")
          case _: Any =>
            response.ctx = ctx
            response.write(s)
        }
      case Failure(t) => log.error("内部错误", t)

    }
  }
}
