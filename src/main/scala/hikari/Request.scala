package hikari

import java.nio.charset.Charset

import io.netty.handler.codec.http.{FullHttpRequest, HttpMethod}

import scala.collection.JavaConverters._

class Request(httpRequest: FullHttpRequest) {

  def method: String = {
    httpRequest.method().name()
  }

  def body: Option[String] = {
    if (httpRequest.method().equals(HttpMethod.POST) || httpRequest.method().equals(HttpMethod.PUT)) {
      Some(httpRequest.content().toString(Charset.forName("UTF-8")))
    } else {
      None
    }
  }

  def url: String = {
    httpRequest.uri()
  }

  def headers: Map[String, String] = {
    httpRequest.headers().asScala.map(x => x.getKey -> x.getValue).toMap
  }

  def header(name: String): Option[String] = {
    headers.get(name)
  }

  def pathParam(name: String): String = {
    pathPattern.get(name).mkString("")
  }


  var pathPattern: Option[MultiParams] = None


}
