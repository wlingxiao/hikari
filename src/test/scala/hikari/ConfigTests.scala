package hikari

import org.scalatest.{FunSuite, Matchers}

class ConfigTests extends FunSuite with Config with Matchers {
  override val configFile: String = "hikari-config-test"

  test("获取配置中的字符串") {
    getStr("test.path") should be(Some("/static"))
  }

  test("获取配置不存在的值") {
    getStr("test.unset") should be(None)
  }

  test("获取配置中的整型数字") {
    getInt("test.port") should be(Some(10080))
  }

  test("获取配置中的 null") {
    getInt("test.port2") should be(None)
  }
}
