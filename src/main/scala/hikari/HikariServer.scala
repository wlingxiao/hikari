package hikari

import io.netty.bootstrap.ServerBootstrap
import io.netty.channel.nio.NioEventLoopGroup
import io.netty.channel.socket.SocketChannel
import io.netty.channel.socket.nio.NioServerSocketChannel
import io.netty.channel.{ChannelInitializer, ChannelOption}
import io.netty.handler.codec.http.HttpServerCodec
import io.netty.handler.logging.{LogLevel, LoggingHandler}

class HikariServer extends ServerConfig {

  private val defaultPort = 8097

  private val bossGroup = new NioEventLoopGroup(1)
  private val workerGroup = new NioEventLoopGroup()

  final def start(port: Int = defaultPort): Unit = {
    val bootstrap = new ServerBootstrap()
    bootstrap.option[Integer](ChannelOption.SO_BACKLOG, 1024)
    bootstrap.group(bossGroup, workerGroup)
      .channel(classOf[NioServerSocketChannel])
      .handler(new LoggingHandler(LogLevel.INFO))
      .childHandler(new HikariServerInitializer(this))

    val configPort = getPort.getOrElse(port)

    val ch = bootstrap.bind(configPort).sync().channel()
    ch.closeFuture().sync()
  }

  final def shutdown(): Unit = {
    bossGroup.shutdownGracefully()
    workerGroup.shutdownGracefully()
  }

}

object HikariServer {

  val defaultSever = new HikariServer

  def start(port: Int = 8097): Unit = {
    defaultSever.start(port)
  }

  def shutdown(): Unit = {
    defaultSever.shutdown()
  }
}

trait ServerConfig extends Config {

  protected val portKey = "hikari.server.port"

  protected val staticPath = "hikari.static.path"

  protected val staticUrl = "hikari.static.url"

  /**
    * 端口号
    *
    * @return
    */
  def getPort: Option[Int] = getInt(portKey)

  /**
    * 静态文件存放目录
    *
    * @return
    */
  def getStaticPath: Option[String] = getStr(staticPath)

  /**
    * 匹配静态文件的请求链接
    *
    * @return
    */
  def getStaticUrl: Option[String] = getStr(staticUrl)
}

class HikariServerInitializer(serverConfig: ServerConfig) extends ChannelInitializer[SocketChannel] {
  override def initChannel(ch: SocketChannel): Unit = {
    val p = ch.pipeline()
    p.addLast(new HttpServerCodec())
    p.addLast("dispatcher", new DispatchHandler(serverConfig))
  }
}