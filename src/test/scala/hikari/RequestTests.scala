package hikari

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
}
