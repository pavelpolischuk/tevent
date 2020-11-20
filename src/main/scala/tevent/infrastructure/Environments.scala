package tevent.infrastructure

import tevent.infrastructure.repository.{Db, SlickEventsRepository, SlickOrganizationsRepository, SlickUsersRepository, Tables}
import tevent.infrastructure.service.Crypto
import tevent.service._
import zio.clock.Clock
import zio.{ULayer, URLayer, ZLayer}

object Environments {
  type HttpServerEnvironment = Configuration.Config with Clock with Crypto
  type AppEnvironment = HttpServerEnvironment with Services

  private val httpServerEnvironment: ULayer[HttpServerEnvironment] = Configuration.live ++ Clock.live >+> Crypto.bcrypt

  private val dbWithTables = Configuration.live >>> Db.fromConfig >+> Tables.live >>> Tables.create
  private val repositories = SlickUsersRepository.live ++ SlickEventsRepository.live ++ SlickOrganizationsRepository.live
  private val services = UsersService.live ++ EventsService.live ++ OrganizationsService.live
  private val servicesDone: URLayer[Crypto, Services with Crypto] = ZLayer.identity[Crypto] ++ (dbWithTables >>> repositories >>> services) >+> AuthService.live

  val appEnvironment: ULayer[AppEnvironment] = httpServerEnvironment >+> servicesDone
}
