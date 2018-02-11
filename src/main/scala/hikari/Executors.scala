package hikari

import io.netty.util.concurrent.DefaultEventExecutorGroup

import scala.concurrent.ExecutionContext

object Executors {
  val group = new DefaultEventExecutorGroup(16)

  implicit val executionContext = new ExecutionContext {

    def execute(runnable: Runnable) {
      group.submit(runnable)
    }

    def reportFailure(t: Throwable) {}
  }

}
