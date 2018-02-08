import hikari.matcher.MultiMap

package object hikari {
  type MultiParams = MultiMap

  type Action = (Request, Response) => Any

  type FilterAction = (Request, Response) => Unit
}
