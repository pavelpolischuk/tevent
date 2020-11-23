package tevent.http.endpoints

import org.http4s.dsl.Http4sDsl
import org.http4s.server.{AuthMiddleware, Router}
import org.http4s.{AuthedRoutes, HttpRoutes}
import tevent.domain.model.User
import tevent.http.model.organization.{OrgParticipationData, OwnOrgParticipationRequest}
import tevent.http.model.user.UserData
import tevent.service.{EventsService, ParticipationService, UsersService}
import zio._
import zio.interop.catz.taskConcurrentInstance

final class UserEndpoint[R <: UsersService with ParticipationService with EventsService] {
  type Task[A] = RIO[R, A]

  private val prefixPath = "/user"
  private val dsl = Http4sDsl[Task]
  import dsl._

  private val httpRoutes = AuthedRoutes.of[User, Task] {
    case GET -> Root as user => Ok(UserData(user.name, user.email))
    case request@PUT -> Root as user => request.req.decode[UserData] { form =>
      UsersService.update(user.id, form.name, form.email).foldM(errorMapper, _ => Ok())
    }
    case GET -> Root / "events" as user =>
      EventsService.getByUser(user.id).foldM(errorMapper, Ok(_))
    case GET -> Root / "organizations" as user =>
      ParticipationService.getOrganizations(user.id).foldM(errorMapper,
        r => Ok(r.map(OrgParticipationData.mapperTo)))
    case GET -> Root / "requests" as user =>
      ParticipationService.getOwnRequests(user.id).foldM(errorMapper,
        r => Ok(r.map(OwnOrgParticipationRequest.mapperTo)))
  }

  def routes(implicit middleware: AuthMiddleware[Task, User]): HttpRoutes[Task] = Router(
    prefixPath -> httpRoutes
  )
}
