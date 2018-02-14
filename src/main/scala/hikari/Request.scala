package hikari

import java.net.URI
import java.nio.charset.Charset
import java.util.Locale

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.scala.DefaultScalaModule
import io.netty.handler.codec.http.HttpHeaderNames.COOKIE
import io.netty.handler.codec.http.cookie.ServerCookieDecoder
import io.netty.handler.codec.http.multipart.InterfaceHttpData.HttpDataType
import io.netty.handler.codec.http.multipart.{Attribute, HttpPostRequestDecoder, MixedFileUpload}
import io.netty.handler.codec.http.{FullHttpRequest, HttpMethod, QueryStringDecoder}
import io.netty.util.ReferenceCountUtil
import org.slf4j.LoggerFactory

import scala.collection.JavaConverters._
import scala.collection.mutable
import scala.collection.mutable.ListBuffer
import scala.reflect.{ClassTag, classTag}

class Request(httpRequest: FullHttpRequest) {

  private val CONTENT_TYPE = "Content-Type"

  private val log = LoggerFactory.getLogger(this.getClass)

  private lazy val objectMapper = {
    val mapper = new ObjectMapper()
    mapper.registerModule(DefaultScalaModule)
    mapper
  }

  def method: String = {
    httpRequest.method().name()
  }

  def body[T](implicit mf: ClassTag[T]): Option[T] = {

    mf match {
      case m if mf == classTag[ByteBuf] =>
        val content = httpRequest.content()
        Option(ByteBuf(content, contentType.getOrElse("text/plain"))).asInstanceOf[Option[T]]
      case _ =>
        if (httpRequest.method().equals(HttpMethod.POST) || httpRequest.method().equals(HttpMethod.PUT)) {
          if (header("Content-Type".toLowerCase()).isDefined && header("Content-Type".toLowerCase).get.contains("application/json")) {
            val b = httpRequest.content().toString(Charset.forName("UTF-8"))
            Option(objectMapper.readValue(b, mf.runtimeClass).asInstanceOf[T])
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

  def forms(name: String): Option[List[Any]] = {
    val decoder = new HttpPostRequestDecoder(httpRequest)
    try {
      val data = decoder.getBodyHttpDatas(name)
      if (data != null) {
        val ret = ListBuffer[Any]()
        for (x <- data.asScala) {
          ReferenceCountUtil.retain(x)
          if (x.getHttpDataType == HttpDataType.Attribute) {
            val y = x.asInstanceOf[Attribute]
            ret += y.getValue
          } else if (x.getHttpDataType == HttpDataType.FileUpload) {
            val y = x.asInstanceOf[MixedFileUpload]
            ret += y
          }
        }
        if (ret.nonEmpty) {
          Some(ret.toList)
        } else None
      } else None
    } finally {
      decoder.destroy()
    }
  }

  def form[T](name: String)(implicit ct: ClassTag[T]): Option[T] = {
    val f = forms(name)
    if (f.isDefined && f.get.nonEmpty) {
      val e = f.get.head
      e match {
        case x: String =>
          ct match {
            case _ if ct == classTag[String] => Option(x).asInstanceOf[Option[T]]
            case _ if ct == classTag[Int] => Option(Integer.parseInt(x)).asInstanceOf[Option[T]]
            case _ if ct == classTag[Long] => Option(java.lang.Long.parseLong(x)).asInstanceOf[Option[T]]
            case _ if ct == classTag[Double] => Option(java.lang.Double.parseDouble(x)).asInstanceOf[Option[T]]
            case _ if ct == classTag[Byte] => Option(java.lang.Byte.parseByte(x)).asInstanceOf[Option[T]]
            // case _ if ct == classTag[Float] => Option(java.lang.Float.parseFloat(x)).asInstanceOf[Option[T]]
            case _ => throw new UnsupportedOperationException(s"不能将参数转换为指定类型：${ct.runtimeClass.getName}")
          }
        case m: MixedFileUpload if ct == classTag[MixedFileUpload] => Option(m).asInstanceOf[Option[T]]
        case _ => Option(e).asInstanceOf[Option[T]]
      }
    } else None

  }

  def forms: Option[List[Map[String, Any]]] = {
    val decoder = new HttpPostRequestDecoder(httpRequest)
    try {
      val data = decoder.getBodyHttpDatas()
      if (data != null) {
        val map = mutable.HashMap[String, Any]()
        val ret = ListBuffer[Map[String, Any]]()
        for (x <- data.asScala) {
          ReferenceCountUtil.retain(x)
          if (x.getHttpDataType == HttpDataType.Attribute) {
            val y = x.asInstanceOf[Attribute]
            map(y.getName) = y.getValue
          } else if (x.getHttpDataType == HttpDataType.FileUpload) {
            val y = x.asInstanceOf[MixedFileUpload]
            map(y.getName) = y
          } else {
            log.warn(s"不明确的请求体参数类型: ${x.getName}")
          }
          ret += map.toMap
        }
        if (ret.nonEmpty) {
          Some(ret.toList)
        } else None
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
    httpRequest.headers().asScala.map(x => x.getKey.toLowerCase(Locale.ENGLISH) -> x.getValue).toMap
  }

  def header(name: String): Option[String] = {
    headers.get(name)
  }

  def pathParam(name: String): String = {
    pathPattern.get(name).mkString("")
  }

  def contentType: Option[String] = header(CONTENT_TYPE.toLowerCase())

  var pathPattern: Option[MultiParams] = None


  def cookies: List[Cookie] = {
    val cookieString = httpRequest.headers().get(COOKIE)
    if (cookieString != null) {
      val cookies = ServerCookieDecoder.STRICT.decode(cookieString)
      cookies.asScala.map(Cookie.nettyCookieToCookie).toList
    } else Nil
  }

  def cookie(name: String): Option[Cookie] = {
    cookies.find(x => x.name == name)
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
