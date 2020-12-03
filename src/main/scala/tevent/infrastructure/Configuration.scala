package tevent.infrastructure

import pureconfig.ConfigSource
import pureconfig.generic.auto._
import zio.{Has, ULayer, ZIO, ZLayer}

object Configuration {
  type Config = Has[Configuration.DbConfig] with Has[Configuration.HttpServerConfig] with Has[Configuration.GmailConfig]

  final case class DbConfig(driver: String, profile: String, url: String, user: String, password: String)
  final case class HttpServerConfig(host: String, port: Int, path: String, secret: String)
  final case class GmailConfig(sender: String, secret: String)
  final case class AppConfig(database: DbConfig, httpServer: HttpServerConfig, gmail: GmailConfig)

  val live: ULayer[Config] = ZIO
    .effect(ConfigSource.default.loadOrThrow[AppConfig])
    .map(c => {
      val port = sys.env.get("PORT").flatMap(_.toIntOption).getOrElse(c.httpServer.port)
      val sender = sys.env.getOrElse("MSENDER", c.gmail.sender)
      val secret = sys.env.getOrElse("MSECRET", c.gmail.secret)
      Has(c.database) ++ Has(c.httpServer.copy(port = port)) ++ Has(GmailConfig(sender, secret))
    })
    .orDie.toLayerMany
}
