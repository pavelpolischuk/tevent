package tevent.main

import tevent.core.{Config, Db}
import tevent.notification.{Email, Notification}
import tevent.user.repository.{SlickUsersRepository, UsersRepository}
import tevent.user.service.{Auth, Crypto, Users}
import tevent.{events, organizations}
import zio.clock.Clock
import zio.{ULayer, ZLayer}

object Environments {
  type HttpServerEnvironment = Config with Clock with Crypto with Email
  type Repositories = events.Repositories with organizations.Repositories with UsersRepository
  type Services = Auth with Users with Notification with events.Services with organizations.Services
  type AppEnvironment = HttpServerEnvironment with Repositories with Services

  private val httpServerEnvironment: ULayer[HttpServerEnvironment] = Config.fromEnv ++ Clock.live >+> Crypto.bcrypt ++ Email.live
  private val testServerEnvironment: ULayer[HttpServerEnvironment] = Config.fromFile ++ Clock.live >+> Crypto.bcrypt ++ Email.option

  private val db = Db.fromConfig >+> Tables.live >>> Tables.createTables
  private val repositories = SlickUsersRepository.live ++ organizations.repositories ++ events.repositories
  private val services = ZLayer.identity[HttpServerEnvironment with Repositories] >+>
    Users.live >+> organizations.services >+> Notification.live >+> events.services >+> Auth.live

  val appEnvironment: ULayer[AppEnvironment] = httpServerEnvironment >+> (db >>> repositories) >+> services
  val testEnvironment: ULayer[AppEnvironment] = testServerEnvironment >+> (db >>> repositories) >+> services
}
