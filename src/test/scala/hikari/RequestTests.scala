package hikari

import io.netty.buffer.Unpooled
import io.netty.handler.codec.http.HttpHeaderNames.{CONTENT_TYPE, COOKIE}
import io.netty.handler.codec.http._
import org.mockito.BDDMockito._
import org.mockito.Mockito._
import org.scalatest.{BeforeAndAfter, FunSuite, Matchers}

class RequestTests extends FunSuite with Matchers with BeforeAndAfter {

  private var fullRequest: FullHttpRequest = _

  before {
    fullRequest = mock(classOf[FullHttpRequest])
  }

  test("获取请求方法") {
    given(fullRequest.method()).willReturn(HttpMethod.GET)
    val request = new Request(fullRequest)
    request.method.equalsIgnoreCase("get") should be(true)
  }

  test("获取请求路径，不包含查询参数") {
    given(fullRequest.uri()).willReturn("/users")
    val request = new Request(fullRequest)
    request.path should equal("/users")
  }

  test("获取请求路径，包含查询参数") {
    given(fullRequest.uri()).willReturn("/users?name=test")
    val request = new Request(fullRequest)
    request.path should equal("/users")
  }

  test("获取所有请求头") {
    val headers = new DefaultHttpHeaders()
    headers.set(HttpHeaderNames.CONTENT_TYPE, "application/json")
    headers.set("test-header", "test-header")
    given(fullRequest.headers()).willReturn(headers)
    val request = new Request(fullRequest)
    request.headers.size should be(2)
  }

  test("获取 content-type，参数名称为 content-type") {
    val headers = new DefaultHttpHeaders()
    headers.set(HttpHeaderNames.CONTENT_TYPE, "application/json")
    given(fullRequest.headers()).willReturn(headers)
    val request = new Request(fullRequest)
    request.contentType should be(Some("application/json"))
  }

  test("获取 content-type，参数名称为 Content-Type，包含部分大写字母") {
    val headers = new DefaultHttpHeaders()
    headers.set("Content-Type", "application/json")
    given(fullRequest.headers()).willReturn(headers)
    val request = new Request(fullRequest)
    request.contentType should be(Some("application/json"))
  }

  test("获取查询参数，链接中有查询参数") {
    given(fullRequest.uri()).willReturn("/test?name=test")
    val request = new Request(fullRequest)
    request.query("name") should be(Some(List("test")))
  }


  test("获取查询参数，链接中没有查询参数") {
    given(fullRequest.uri()).willReturn("/test")
    val request = new Request(fullRequest)
    request.query("name") should be(None)
  }


  test("获取请求体参数，content-type 为 application/x-www-form-urlencoded") {
    val byteBuf = Unpooled.wrappedBuffer("name=test".getBytes)
    val df = new DefaultFullHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.POST, "/test", byteBuf)
    df.headers().set(CONTENT_TYPE, "application/x-www-form-urlencoded")
    val request = new Request(df)
    request.forms("name") should be(Some(List("test")))
  }

  test("获取请求体参数，请求体中不包含任何参数") {
    val df = new DefaultFullHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.POST, "/test")
    df.headers().set(CONTENT_TYPE, "application/x-www-form-urlencoded")
    val request = new Request(df)
    request.forms("name") should be(None)
  }

  test("获取单个请求体参数，并将该参数转化为制定类型 Int") {
    val byteBuf = Unpooled.wrappedBuffer("name=1123".getBytes)
    val df = new DefaultFullHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.POST, "/test", byteBuf)
    df.headers().set(CONTENT_TYPE, "application/x-www-form-urlencoded")
    val request = new Request(df)
    request.form[Int]("name") should be(Some(1123))

  }

  test("获取请求体参数，content-type 为 application/form-data，不包含文件") {
    val byteBuf = Unpooled.wrappedBuffer("name=test".getBytes)
    val df = new DefaultFullHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.POST, "/test", byteBuf)
    df.headers().set(CONTENT_TYPE, "form-data")
    val request = new Request(df)
    request.forms("name") should be(Some(List("test")))
  }

  test("获取所有 Cookie，请求中包含 Cookie header") {
    val df = new DefaultFullHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.POST, "/test")
    df.headers().set(COOKIE, "name=test;name1=test1")
    val request = new Request(df)
    request.cookies.length should be(2)
  }

  test("获取所有 Cookie，请求中没有 Cookie") {
    val df = new DefaultFullHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.POST, "/test")
    val request = new Request(df)
    request.cookies.length should be(0)
  }

  test("通过 Cookie 名称获取 Cookie, 请求中包含 Cookie header") {
    val df = new DefaultFullHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.POST, "/test")
    df.headers().set(COOKIE, "name=test;name1=test1")
    val request = new Request(df)
    request.cookie("name").get.value should equal("test")
  }

  test("通过 Cookie 名称获取 Cookie, 请求中没有 Cookie") {
    val df = new DefaultFullHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.POST, "/test")
    val request = new Request(df)
    request.cookie("name") should be(None)
  }

}
