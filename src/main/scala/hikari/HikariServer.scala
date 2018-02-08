package hikari

import io.netty.bootstrap.ServerBootstrap
import io.netty.channel.nio.NioEventLoopGroup
import io.netty.channel.socket.SocketChannel
import io.netty.channel.socket.nio.NioServerSocketChannel
import io.netty.channel.{ChannelInitializer, ChannelOption}
import io.netty.handler.codec.http.{HttpObjectAggregator, HttpServerCodec, HttpServerExpectContinueHandler}
import io.netty.handler.logging.{LogLevel, LoggingHandler}

object HikariServer {

  private val bossGroup = new NioEventLoopGroup(1)
  private val workerGroup = new NioEventLoopGroup()

  def start(port: Int = 8097): Unit = {
    val bootstrap = new ServerBootstrap()
    bootstrap.option[Integer](ChannelOption.SO_BACKLOG, 1024)
    bootstrap.group(bossGroup, workerGroup)
      .channel(classOf[NioServerSocketChannel])
      .handler(new LoggingHandler(LogLevel.INFO))
      .childHandler(new HikariServerInitializer)
    val ch = bootstrap.bind(port).sync().channel()
    ch.closeFuture().sync()
  }

  def shutdown(): Unit = {
    bossGroup.shutdownGracefully()
    workerGroup.shutdownGracefully()
  }
}

class HikariServerInitializer extends ChannelInitializer[SocketChannel] {
  override def initChannel(ch: SocketChannel): Unit = {
    val p = ch.pipeline()
    p.addLast(new HttpServerCodec())
    p.addLast(new HttpObjectAggregator(Short.MaxValue))
    p.addLast(new HttpServerExpectContinueHandler())
    p.addLast(new BasicHandler)
  }
}