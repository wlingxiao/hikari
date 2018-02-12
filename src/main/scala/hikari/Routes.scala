package hikari

object Routes {

  def get = InternalRoute.get _

  def post = InternalRoute.post _

  def put = InternalRoute.put _

  def delete = InternalRoute.delete _

  def options = InternalRoute.options _

}

object Filters {

  def before = InternalRoute.before _

  def after = InternalRoute.after _

}