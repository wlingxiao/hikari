package hikari

import io.netty.buffer.{Unpooled, ByteBuf => NettyByteBuf}

class ByteBuf(val buffer: NettyByteBuf, val contentType: String) {

  def this(content: Array[Byte], contentType: String) {
    this(Unpooled.wrappedBuffer(content), contentType)
  }

}

object ByteBuf {

  def apply(buf: NettyByteBuf, contentType: String): ByteBuf = new ByteBuf(buf, contentType)

  def apply(array: Array[Byte], contentType: String): ByteBuf = new ByteBuf(array, contentType)

}
