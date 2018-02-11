package hikari

import io.netty.buffer.Unpooled
import io.netty.channel.{ChannelFutureListener, ChannelHandlerContext, SimpleChannelInboundHandler}
import io.netty.handler.codec.http.{DefaultFullHttpResponse, FullHttpRequest, HttpResponseStatus}
import io.netty.handler.codec.http.HttpVersion.HTTP_1_1
import scala.concurrent.Future
import scala.util.{Failure, Success}

import Executors._

class AsyncHandler(future: Future[_]) extends SimpleChannelInboundHandler[FullHttpRequest] {
  override def channelRead0(ctx: ChannelHandlerContext, msg: FullHttpRequest): Unit = {

    println("how are you")
    future.onComplete {
      case Success(s) =>
        val response = new DefaultFullHttpResponse(HTTP_1_1, HttpResponseStatus.valueOf(200), Unpooled.wrappedBuffer(s.toString.getBytes))
        ctx.writeAndFlush(response).addListener(ChannelFutureListener.CLOSE)
      case Failure(t) => t.printStackTrace()
    }
  }
}
