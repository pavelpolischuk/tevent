package tevent.main

import tevent.core.{Config, Db}
import tevent.notification.{Email, Notification}
import tevent.user.service.Crypto
import tevent.{events, organizations, user}
import zio.clock.Clock
import zio.{ULayer, ZLayer}

object Environments {
  type HttpServerEnvironment = Config with Clock with Crypto with Email
  type Repositories = events.Repositories with organizations.Repositories with user.Repositories
  type Services = user.Services with Notification with events.Services with organizations.Services
  type AppEnvironment = HttpServerEnvironment with Repositories with Services

  private val httpServerEnvironment: ULayer[HttpServerEnvironment] = Config.fromEnv ++ Clock.live >+> Crypto.bcrypt ++ Email.live
  private val testServerEnvironment: ULayer[HttpServerEnvironment] = Config.fromFile ++ Clock.live >+> Crypto.bcrypt ++ Email.option

  private val db = Db.fromConfig >+> Tables.live >>> Tables.createTables
  private val repositories = user.repositories ++ organizations.repositories ++ events.repositories
  private val services = ZLayer.identity[HttpServerEnvironment with Repositories] >+>
    organizations.services >+> Notification.live >+> events.services >+> user.services

  val appEnvironment: ULayer[AppEnvironment] = httpServerEnvironment >+> (db >>> repositories) >+> services
  val testEnvironment: ULayer[AppEnvironment] = testServerEnvironment >+> (db >>> repositories) >+> services
}
