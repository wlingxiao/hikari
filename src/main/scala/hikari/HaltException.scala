package hikari

class HaltException(val code: Int, val msg: String) extends RuntimeException
