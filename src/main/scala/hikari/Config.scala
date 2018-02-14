package hikari

import com.typesafe.config.{ConfigFactory, Config => TypesafeConfig}

trait Config {

  protected val configFile = "hikari"

  protected lazy val config: TypesafeConfig = ConfigFactory.load(configFile)

  def getStr(key: String): Option[String] = {
    if (config.hasPath(key)) {
      Some(config.getString(key))
    } else None
  }

  def getInt(key: String): Option[Int] = {
    if (config.hasPath(key)) {
      Some(config.getInt(key))
    } else None
  }
}


