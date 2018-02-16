package hikari

import java.nio.charset.Charset

import com.fasterxml.jackson.annotation.JsonInclude.Include
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.scala.DefaultScalaModule
import io.netty.buffer.Unpooled
import io.netty.handler.codec.http.HttpVersion.HTTP_1_1
import io.netty.handler.codec.http._

trait ContentTransformer[T, H <: HttpResponse] {

  def transform(content: T): H

}

class StringTransformer(fhp: FullHttpRequest, status: Int, contentType: String, charset: Charset = Charset.forName("UTF-8")) extends ContentTransformer[String, DefaultFullHttpResponse] {
  def transform(content: String): DefaultFullHttpResponse = {
    val response = new DefaultFullHttpResponse(HTTP_1_1, HttpResponseStatus.valueOf(status), Unpooled.wrappedBuffer(content.getBytes(charset)))
    response.headers().add(HttpHeaderNames.CONTENT_TYPE, contentType)
    HttpUtil.setContentLength(response, response.content().readableBytes())
    response
  }
}

class JsonTransformer(status: Int, contentType: String = "application/json", charset: Charset = Charset.forName("UTF-8")) extends ContentTransformer[Any, HttpResponse] {

  private var _mapper = {
    val ow = new ObjectMapper()
    ow.setSerializationInclusion(Include.NON_NULL)
    ow.registerModule(DefaultScalaModule)
    ow.writer.withDefaultPrettyPrinter
    ow
  }

  def mapper_(mapper: ObjectMapper): Unit = {
    this._mapper = mapper
  }

  def transform(content: Any): HttpResponse = {
    val json = _mapper.writeValueAsBytes(content)
    val resp = new DefaultFullHttpResponse(HTTP_1_1, HttpResponseStatus.valueOf(status), Unpooled.wrappedBuffer(json))
    resp.headers().add(HttpHeaderNames.CONTENT_TYPE, contentType + ";" + charset.displayName())
    resp
  }
}
