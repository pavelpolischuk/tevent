package tevent.infrastructure

import tevent.domain.repository.Repositories
import tevent.infrastructure.repository.{Db, SlickEventsRepository, SlickOrganizationsRepository, SlickUsersRepository, Tables}
import tevent.infrastructure.service.{Crypto, Email, EmailSender}
import tevent.service.{NotificationService, _}
import zio.clock.Clock
import zio.{ULayer, URLayer, ZLayer}

object Environments {
  type HttpServerEnvironment = Configuration.Config with Clock with Crypto with Email
  type AppEnvironment = HttpServerEnvironment with Services

  private val httpServerEnvironment: ULayer[HttpServerEnvironment] = Configuration.fromEnv ++ Clock.live >+> Crypto.bcrypt ++ EmailSender.live
  private val testServerEnvironment: ULayer[HttpServerEnvironment] = Configuration.fromFile ++ Clock.live >+> Crypto.bcrypt ++ EmailSender.option

  private val dbWithTables = ZLayer.identity[Configuration.Config] >>> Db.fromConfig >+> Tables.live >>> Tables.create
  private val repositories = SlickUsersRepository.live ++ SlickEventsRepository.live ++ SlickOrganizationsRepository.live
  private val services = ZLayer.identity[Repositories] ++ ZLayer.identity[Email] >+> NotificationService.live ++ ParticipationService.live >>>
    (ZLayer.identity[ParticipationService] ++ ZLayer.identity[NotificationService] ++ EventsService.live ++ OrganizationsService.live ++ UsersService.live)
  private val servicesDone: URLayer[Configuration.Config with Email with Crypto, Configuration.Config with Email with Crypto with Services] =
    ZLayer.identity[Configuration.Config] ++ ZLayer.identity[Email] ++ ZLayer.identity[Crypto] >+> (
      (dbWithTables >>> repositories) ++ ZLayer.identity[Email] >>> services) >+> AuthService.live

  val appEnvironment: ULayer[AppEnvironment] = httpServerEnvironment >+> servicesDone
  val testEnvironment: ULayer[AppEnvironment] = testServerEnvironment >+> servicesDone
}
