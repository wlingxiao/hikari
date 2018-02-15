package hikari

import io.netty.util.concurrent.FastThreadLocal

object routes {

  def get = InternalRoute.get _

  def post = InternalRoute.post _

  private val requestHolder = new FastThreadLocal[Request]()

  private val responseHolder = new FastThreadLocal[Response]()

  private[hikari] def setRequest(request: Request): Unit = {
    requestHolder.set(request)
  }

  private[hikari] def setResponse(response: Response): Unit = {
    responseHolder.set(response)
  }

  def clearAll(): Unit = {
    requestHolder.remove()
    responseHolder.remove()
  }

  def request: Request = requestHolder.get()

  def response: Response = responseHolder.get()

}

object Filters {

  def before = InternalRoute.before _

  def after = InternalRoute.after _

}