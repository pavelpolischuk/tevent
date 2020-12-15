package tevent.core

import pureconfig.ConfigSource
import zio.{Has, ULayer, ZIO}
import pureconfig.generic.auto._

object Config {
  final case class DbConfig(driver: String, profile: String, url: String, user: String, password: String)

  final case class HttpServerConfig(host: String, port: Int, path: String, secret: String)

  final case class GmailConfig(sender: String, secret: String)

  final case class AppConfig(database: DbConfig, httpServer: HttpServerConfig, gmail: GmailConfig)

  val fromFile: ULayer[Config] = ZIO
    .effect(ConfigSource.default.loadOrThrow[AppConfig])
    .map(c => Has(c.database) ++ Has(c.httpServer) ++ Has(c.gmail))
    .orDie.toLayerMany

  val fromEnv: ULayer[Config] = ZIO
    .effect(sys.env)
    .map(env => {
      val http = HttpServerConfig(
        env.getOrElse("HOST", "0.0.0.0"),
        env.get("PORT").flatMap(_.toIntOption).getOrElse(8080),
        env.getOrElse("ROOT_PATH", "/api/v1"),
        env.getOrElse("AUTH_SECRET", "1122334455667788990011223344556677889900")
      )

      val db = DbConfig(
        env.getOrElse("JDBC_DATABASE_DRIVER", "org.h2.Driver"),
        env.getOrElse("JDBC_DATABASE_PROFILE", "slick.jdbc.H2Profile$"),
        env.getOrElse("JDBC_DATABASE_URL", "jdbc:h2:mem:dev;DB_CLOSE_DELAY=-1"),
        env.getOrElse("JDBC_DATABASE_USERNAME", ""),
        env.getOrElse("JDBC_DATABASE_PASSWORD", "")
      )

      val gmail = GmailConfig(
        env.getOrElse("MSENDER", ""),
        env.getOrElse("MSECRET", "")
      )

      Has(db) ++ Has(http) ++ Has(gmail)
    })
    .orDie.toLayerMany
}
