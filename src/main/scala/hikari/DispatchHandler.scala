package hikari

import io.netty.channel.{ChannelHandlerContext, SimpleChannelInboundHandler}
import io.netty.handler.codec.http.{HttpObject, HttpObjectAggregator, HttpRequest}
import io.netty.util.ReferenceCountUtil

class DispatchHandler(serverConfig: ServerConfig) extends SimpleChannelInboundHandler[HttpObject] {
  override def channelRead0(ctx: ChannelHandlerContext, msg: HttpObject): Unit = {
    msg match {
      case req: HttpRequest =>
        if (serverConfig.getStaticPath.isDefined) {
          if (req.uri().startsWith(serverConfig.getStaticUrl.getOrElse("/static"))) {
            ctx.channel().pipeline().replace(this, "ObjectAggregator", new HttpObjectAggregator(Short.MaxValue))
            ctx.channel().pipeline().addLast(new StaticFileHandler)
          }
        } else {
          ctx.channel().pipeline().replace(this, "ObjectAggregator", new HttpObjectAggregator(Short.MaxValue))
          ctx.channel().pipeline().addLast(new BasicHandler)
        }
        ctx.fireChannelRead(ReferenceCountUtil.retain(msg))
      case _ =>
    }
  }
}
