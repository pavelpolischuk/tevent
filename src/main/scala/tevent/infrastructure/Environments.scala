package tevent.infrastructure

import tevent.domain.repository.Repositories
import tevent.infrastructure.repository.{Db, SlickEventsRepository, SlickOrganizationsRepository, SlickUsersRepository, Tables}
import tevent.service.{EventsService, OrganizationsService, Services, UsersService}
import zio.ULayer
import zio.clock.Clock

object Environments {
  type HttpServerEnvironment = Configuration.Config with Clock
  type AppEnvironment = HttpServerEnvironment with Repositories with Services

  lazy val httpServerEnvironment: ULayer[HttpServerEnvironment] = Configuration.live ++ Clock.live
  lazy val db: ULayer[Db] = Configuration.live >>> Db.fromConfig
  lazy val dbWithTables: ULayer[Db with Tables] = (db >+> Tables.live) >>> Tables.create
  lazy val repositories: ULayer[Repositories] = dbWithTables >>> (SlickUsersRepository.live ++ SlickEventsRepository.live ++ SlickOrganizationsRepository.live)
  lazy val services: ULayer[Services] = repositories >>> (UsersService.live ++ EventsService.live ++ OrganizationsService.live)
  lazy val appEnvironment: ULayer[AppEnvironment] = httpServerEnvironment ++ repositories ++ services
}
