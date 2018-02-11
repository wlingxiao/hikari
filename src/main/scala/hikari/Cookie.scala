package hikari

import io.netty.handler.codec.http.cookie.{DefaultCookie, Cookie => NettyCookie}

case class Cookie(name: String,
                  value: String,
                  domain: String = null,
                  path: String = "/",
                  maxAge: Long = 0,
                  secure: Boolean = true,
                  httpOnly: Boolean = false)

object Cookie {

  implicit def cookieToNettyCookie(c: Cookie): NettyCookie = {
    val dc = new DefaultCookie(c.name, c.value)
    dc.setDomain(c.domain)
    dc.setPath(c.path)
    dc.setMaxAge(c.maxAge)
    dc.setSecure(c.secure)
    dc.setHttpOnly(c.httpOnly)
    dc
  }

  implicit def nettyCookieToCookie(nc: NettyCookie): Cookie = {
    Cookie(nc.name(), nc.value(), nc.domain(), nc.path(), nc.maxAge(), nc.isSecure, nc.isHttpOnly)
  }

}