package tevent.main

import tevent.core.{Config, Db}
import tevent.events.repository.{EventsRepository, SlickEventsRepository}
import tevent.events.service.Events
import tevent.notification.{Email, Notification}
import tevent.organizations.repository.{OrganizationsRepository, SlickOrganizationsRepository}
import tevent.organizations.service.{OrganizationParticipants, Organizations}
import tevent.user.repository.{SlickUsersRepository, UsersRepository}
import tevent.user.service.{Auth, Crypto, Users}
import zio.clock.Clock
import zio.{ULayer, ZLayer}

object Environments {
  type HttpServerEnvironment = Config with Clock with Crypto with Email
  type Repositories = EventsRepository with OrganizationsRepository with UsersRepository
  type Services = Crypto with Auth with Notification with Users with Events with Organizations with OrganizationParticipants
  type AppEnvironment = HttpServerEnvironment with Repositories with Services

  private val httpServerEnvironment: ULayer[HttpServerEnvironment] = Config.fromEnv ++ Clock.live >+> Crypto.bcrypt ++ Email.live
  private val testServerEnvironment: ULayer[HttpServerEnvironment] = Config.fromFile ++ Clock.live >+> Crypto.bcrypt ++ Email.option

  private val db = Db.fromConfig >+> Tables.live >>> Tables.createTables
  private val repositories = SlickUsersRepository.live ++ SlickEventsRepository.live ++ SlickOrganizationsRepository.live
  private val services = ZLayer.identity[HttpServerEnvironment with Repositories] >+>
    (Notification.live ++ OrganizationParticipants.live) >+>
    (ZLayer.identity[OrganizationParticipants with Notification] ++ Events.live ++ Organizations.live ++ Users.live) >+> Auth.live

  val appEnvironment: ULayer[AppEnvironment] = httpServerEnvironment >+> (db >>> repositories) >+> services
  val testEnvironment: ULayer[AppEnvironment] = testServerEnvironment >+> (db >>> repositories) >+> services
}
