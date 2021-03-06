package hikari

import io.netty.buffer.Unpooled
import io.netty.handler.codec.http.HttpHeaderNames.{CONTENT_TYPE, COOKIE}
import io.netty.handler.codec.http._
import org.mockito.BDDMockito._
import org.mockito.Mockito._
import org.scalatest.{BeforeAndAfter, FunSuite, Matchers}

case class RequestTestUser(name: String, age: Int)

class RequestTests extends FunSuite with Matchers with BeforeAndAfter {

  private var fullRequest: FullHttpRequest = _

  before {
    fullRequest = mock(classOf[FullHttpRequest])
  }

  test("获取请求方法") {
    given(fullRequest.method()).willReturn(HttpMethod.GET)
    val request = new Request(fullRequest, null)
    request.method.equalsIgnoreCase("get") should be(true)
  }

  test("获取请求路径，不包含查询参数") {
    given(fullRequest.uri()).willReturn("/users")
    val request = new Request(fullRequest, null)
    request.path should equal("/users")
  }

  test("获取请求路径，包含查询参数") {
    given(fullRequest.uri()).willReturn("/users?name=test")
    val request = new Request(fullRequest, null)
    request.path should equal("/users")
  }

  test("获取所有请求头") {
    val headers = new DefaultHttpHeaders()
    headers.set(HttpHeaderNames.CONTENT_TYPE, "application/json")
    headers.set("test-header", "test-header")
    given(fullRequest.headers()).willReturn(headers)
    val request = new Request(fullRequest, null)
    request.headers.size should be(2)
  }

  test("获取所有请求头，当没有请求头时") {
    val req = new DefaultFullHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.POST, "/test")

    val request = new Request(req, null)
    request.headers.size should be(0)
  }

  test("根据名称获取单个请求头，不区分大小写，参数为小写") {
    val req = new DefaultFullHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.POST, "/test")
    req.headers().add("test-header", "test-header")

    val request = new Request(req, null)
    request.header("test-header") should be(Some("test-header"))

  }

  test("根据名称获取单个请求头，不区分大小写，参数为大写") {
    val req = new DefaultFullHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.POST, "/test")
    req.headers().add("test-header", "test-header")

    val request = new Request(req, null)
    request.header("Test-Header") should be(Some("test-header"))
  }

  test("获取 content-type，参数名称为 content-type") {
    val headers = new DefaultHttpHeaders()
    headers.set(HttpHeaderNames.CONTENT_TYPE, "application/json")
    given(fullRequest.headers()).willReturn(headers)
    val request = new Request(fullRequest, null)
    request.contentType should be(Some("application/json"))
  }

  test("获取 content-type，参数名称为 Content-Type，包含部分大写字母") {
    val headers = new DefaultHttpHeaders()
    headers.set("Content-Type", "application/json")
    given(fullRequest.headers()).willReturn(headers)
    val request = new Request(fullRequest, null)
    request.contentType should be(Some("application/json"))
  }

  test("获取查询参数，链接中有查询参数") {
    given(fullRequest.uri()).willReturn("/test?name=test")
    val request = new Request(fullRequest, null)
    request.query("name") should be(Some(List("test")))
  }

  test("获取查询参数，链接中没有查询参数") {
    given(fullRequest.uri()).willReturn("/test")
    val request = new Request(fullRequest, null)
    request.query("name") should be(None)
  }

  test("获取请求体参数，content-type 为 application/x-www-form-urlencoded") {
    val byteBuf = Unpooled.wrappedBuffer("name=test".getBytes)
    val df = new DefaultFullHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.POST, "/test", byteBuf)
    df.headers().set(CONTENT_TYPE, "application/x-www-form-urlencoded")
    val request = new Request(df, null)
    request.forms("name") should be(Some(List("test")))
  }

  test("获取请求体参数，请求体中不包含任何参数") {
    val df = new DefaultFullHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.POST, "/test")
    df.headers().set(CONTENT_TYPE, "application/x-www-form-urlencoded")
    val request = new Request(df, null)
    request.forms("name") should be(None)
  }

  test("获取单个请求体参数，并将该参数转化为制定类型 Int") {
    val byteBuf = Unpooled.wrappedBuffer("name=1123".getBytes)
    val df = new DefaultFullHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.POST, "/test", byteBuf)
    df.headers().set(CONTENT_TYPE, "application/x-www-form-urlencoded")
    val request = new Request(df, null)
    request.form[Int]("name") should be(Some(1123))

  }

  test("获取请求体参数，content-type 为 application/form-data，不包含文件") {
    val byteBuf = Unpooled.wrappedBuffer("name=test".getBytes)
    val df = new DefaultFullHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.POST, "/test", byteBuf)
    df.headers().set(CONTENT_TYPE, "form-data")
    val request = new Request(df, null)
    request.forms("name") should be(Some(List("test")))
  }

  test("获取所有 Cookie，请求中包含 Cookie header") {
    val df = new DefaultFullHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.POST, "/test")
    df.headers().set(COOKIE, "name=test;name1=test1")
    val request = new Request(df, null)
    request.cookies.length should be(2)
  }

  test("获取所有 Cookie，请求中没有 Cookie") {
    val df = new DefaultFullHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.POST, "/test")
    val request = new Request(df, null)
    request.cookies.length should be(0)
  }

  test("通过 Cookie 名称获取 Cookie, 请求中包含 Cookie header") {
    val df = new DefaultFullHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.POST, "/test")
    df.headers().set(COOKIE, "name=test;name1=test1")
    val request = new Request(df, null)
    request.cookie("name").get.value should equal("test")
  }

  test("通过 Cookie 名称获取 Cookie, 请求中没有 Cookie") {
    val df = new DefaultFullHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.POST, "/test")
    val request = new Request(df, null)
    request.cookie("name") should be(None)
  }

  test("反序列化 application/json 请求体，请求体中包含完整的实体信息") {
    val content = Unpooled.wrappedBuffer("""{"name": "test", "age": 123}""".getBytes)
    val req = new DefaultFullHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.POST, "/test", content)
    req.headers().add("Content-Type", "application/json:charset=utf-8")

    val request = new Request(req, null)
    request.body[RequestTestUser] should be(Some(RequestTestUser("test", 123)))
  }

  test("反序列化 application/json 请求体，请求体中缺少部分信息，不匹配的引用类型字段默认设置为 null") {
    val content = Unpooled.wrappedBuffer("""{"age": 123}""".getBytes)
    val req = new DefaultFullHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.POST, "/test", content)
    req.headers().add("Content-Type", "application/json:charset=utf-8")

    val request = new Request(req, null)
    request.body[RequestTestUser] should be(Some(RequestTestUser(null, 123)))
  }

  test("反序列化 application/json 请求体，实体中缺少部分信息") {
    val content = Unpooled.wrappedBuffer("""{"name": "test", "age": 123, "sex": "female"}""".getBytes)
    val req = new DefaultFullHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.POST, "/test", content)
    req.headers().add("Content-Type", "application/json:charset=utf-8")

    val request = new Request(req, null)
    request.body[RequestTestUser] should be(Some(RequestTestUser("test", 123)))
  }

  test("Content-Type 不为json时和参数类型不为 ByteBuf 时，返回None") {
    val content = Unpooled.wrappedBuffer("""{"name": "test", "age": 123, "sex": "female"}""".getBytes)
    val req = new DefaultFullHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.POST, "/test", content)
    req.headers().add("Content-Type", "application/javascript")

    val request = new Request(req, null)
    request.body[RequestTestUser] should be(None)
  }


}
