package hikari

import java.net.URI
import java.nio.charset.Charset

import io.netty.handler.codec.http.HttpHeaderNames.COOKIE
import io.netty.handler.codec.http.cookie.ServerCookieDecoder
import io.netty.handler.codec.http.multipart.InterfaceHttpData.HttpDataType
import io.netty.handler.codec.http.multipart.{Attribute, HttpPostRequestDecoder}
import io.netty.handler.codec.http.{FullHttpRequest, HttpMethod, QueryStringDecoder}
import org.json4s.jackson.JsonMethods
import org.json4s.{DefaultFormats, Formats}
import org.slf4j.LoggerFactory

import scala.collection.JavaConverters._
import scala.collection.mutable
import scala.collection.mutable.ListBuffer

class ByteBody {}

class Request(httpRequest: FullHttpRequest) {

  private val CONTENT_TYPE = "Content-Type"

  private val log = LoggerFactory.getLogger(this.getClass)

  private implicit val formats: Formats = DefaultFormats

  def method: String = {
    httpRequest.method().name()
  }

  def body[T](implicit mf: Manifest[T]): Option[T] = {

    mf match {
      case m if mf == manifest[ByteBuf] =>
        val content = httpRequest.content()
        Option(ByteBuf(content, contentType.getOrElse("text/plain"))).asInstanceOf[Option[T]]
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

  def query(name: String): Option[List[String]] = {
    val queryDecoder = new QueryStringDecoder(httpRequest.uri())
    queryDecoder.parameters().asScala.get(name) match {
      case Some(l) => Some(l.asScala.toList)
      case None => None
    }
  }

  def form(name: String): Option[List[String]] = {
    val decoder = new HttpPostRequestDecoder(httpRequest)
    val data = decoder.getBodyHttpDatas(name)
    try {
      val ret = ListBuffer[String]()
      for (x <- data.asScala) {
        if (x.getHttpDataType == HttpDataType.Attribute) {
          val y = x.asInstanceOf[Attribute]
          ret += y.getValue
        } else {
          log.warn("不支持文件上传")
        }
      }
      if (ret.nonEmpty) {
        Some(ret.toList)
      } else None
    } finally {
      decoder.destroy()
    }
  }

  def raw: FullHttpRequest = httpRequest

  def path: String = {
    new URI(httpRequest.uri()).getPath
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

  def contentType: Option[String] = header(CONTENT_TYPE.toLowerCase())

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
