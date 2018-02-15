package hikari

object Routes {

  def get = InternalRoute.get _

  def post = InternalRoute.post _

}

object Filters {

  def before = InternalRoute.before _

  def after = InternalRoute.after _

}