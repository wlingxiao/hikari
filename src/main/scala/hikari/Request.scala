package hikari

import java.nio.charset.Charset

import io.netty.handler.codec.http.HttpHeaderNames.COOKIE
import io.netty.handler.codec.http.cookie.ServerCookieDecoder
import io.netty.handler.codec.http.{FullHttpRequest, HttpMethod}
import io.netty.util.AsciiString
import org.json4s.jackson.JsonMethods
import org.json4s.{DefaultFormats, Formats}
import org.slf4j.LoggerFactory

import scala.collection.JavaConverters._
import scala.collection.mutable

class ByteBody {}

class Request(httpRequest: FullHttpRequest) {

  private val CONTENT_TYPE = AsciiString.cached("Content-Type")

  private val log = LoggerFactory.getLogger(this.getClass)

  private implicit val formats: Formats = DefaultFormats

  def method: String = {
    httpRequest.method().name()
  }

  def body[T](implicit mf: Manifest[T]): Option[T] = {

    mf match {
      case m if mf == manifest[ByteBody] =>
        log.warn("不支持二进制文件的请求体")
        None
      case _ =>
        if (httpRequest.method().equals(HttpMethod.POST) || httpRequest.method().equals(HttpMethod.PUT)) {
          if (header("Content-Type".toLowerCase()).isDefined && header("Content-Type".toLowerCase).get.contains("application/json")) {
            val b = httpRequest.content().toString(Charset.forName("UTF-8"))
            JsonMethods.parse(b).extractOpt[T]
          } else {
            None
          }
        } else {
          None
        }
    }
  }

  def raw: FullHttpRequest = httpRequest

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


  def cookies(): List[Cookie] = {
    val cookieString = httpRequest.headers().get(COOKIE)
    if (cookieString != null) {
      val cookies = ServerCookieDecoder.STRICT.decode(cookieString)
      cookies.asScala.map(Cookie.nettyCookieToCookie).toList
    } else Nil
  }

  def cookie(name: String): Option[Cookie] = {
    cookies().find(x => x.name == name)
  }

  private val paramsMap = mutable.HashMap[Any, Any]()

  def params(key: Any, value: Any): Unit = {
    paramsMap(key) = value
  }

  def params[T](key: Any)(implicit mf: Manifest[T]): Option[T] = {
    paramsMap.get(key) match {
      case Some(x) if mf.runtimeClass.isInstance(x) => Some(x.asInstanceOf[T])
      case _ => None
    }
  }

}
