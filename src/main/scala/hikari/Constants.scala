package hikari

import io.netty.util.AttributeKey

object Constants {

  val REQUEST_KEY: AttributeKey[Request] = AttributeKey.valueOf[Request]("hikari.request")

  val RESPONSE_KEY: AttributeKey[Response] = AttributeKey.valueOf[Response]("hikari.response")

}
