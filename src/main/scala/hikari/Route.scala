package hikari

import hikari.matcher.{PathPattern, SinatraPathPatternParser}

object Route {

  private[hikari] val getRoute = scala.collection.mutable.HashMap[PathPattern, Action]()

  private[hikari] val beforeFilters = scala.collection.mutable.HashMap[PathPattern, FilterAction]()

  private[hikari] val afterMap = scala.collection.mutable.HashMap[PathPattern, FilterAction]()

  def get(path: String)(action: Action): Unit = {
    getRoute(SinatraPathPatternParser(path)) = action
  }

  def before(path: String)(action: FilterAction): Unit = {
    beforeFilters(SinatraPathPatternParser(path)) = action
  }

  def after(path: String)(action: FilterAction): Unit = {
    afterMap(SinatraPathPatternParser(path)) = action
  }

}
