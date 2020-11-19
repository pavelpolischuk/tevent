package tevent.infrastructure

import pureconfig.ConfigSource
import pureconfig.generic.auto._
import zio.{Has, ULayer, ZIO, ZLayer}

object Configuration {
  type Config = Has[Configuration.DbConfig] with Has[Configuration.HttpServerConfig]

  final case class DbConfig(driver: String, profile: String, url: String, user: String, password: String)
  final case class HttpServerConfig(host: String, port: Int, path: String)
  final case class AppConfig(database: DbConfig, httpServer: HttpServerConfig)

  val live: ULayer[Config] = ZLayer.fromEffectMany(
    ZIO
      .effect(ConfigSource.default.loadOrThrow[AppConfig])
      .map(c => Has(c.database) ++ Has(c.httpServer))
      .orDie
  )
}
