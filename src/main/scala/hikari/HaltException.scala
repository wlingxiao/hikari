package hikari

import scala.util.control.NoStackTrace

class HaltException(val code: Int, val msg: String) extends RuntimeException with NoStackTrace
