package hikari

import java.io.{File, RandomAccessFile}

import io.netty.buffer.{ByteBuf => NettyByteBuf}

class ByteBuf(val buffer: NettyByteBuf, val contentType: String)

object ByteBuf {

  def apply(buf: NettyByteBuf, contentType: String): ByteBuf = new ByteBuf(buf, contentType)

}

class Binary(val file: RandomAccessFile, val contentType: String) {

  def this(file: File, contentType: String) {
    this(new RandomAccessFile(file, "r"), contentType)
  }

}

object Binary {

  def apply(file: File, contentType: String) = new Binary(file, contentType)

  def apply(file: RandomAccessFile, contentType: String) = new Binary(file, contentType)
}