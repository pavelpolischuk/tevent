package tevent.http.endpoints

import org.http4s.dsl.Http4sDsl
import org.http4s.server.{AuthMiddleware, Router}
import org.http4s.{AuthedRoutes, HttpRoutes}
import tevent.domain.model.User
import tevent.http.model.UserData
import tevent.http.model.EventParticipation.eventParticipationTupleEncoder
import tevent.http.model.OrgParticipation.orgParticipationTupleEncoder
import tevent.service.{ParticipationService, UsersService}
import zio._
import zio.interop.catz.taskConcurrentInstance

final class UsersEndpoint[R <: UsersService with ParticipationService] {
  type Task[A] = RIO[R, A]

  private val prefixPath = "/users"
  private val dsl = Http4sDsl[Task]
  import dsl._

  private val httpRoutes = AuthedRoutes.of[User, Task] {
    case GET -> Root / LongVar(id) as user =>
      if (user.id == id) Ok(UserData(user.name, user.email)) else Forbidden()
    case request@PUT -> Root / LongVar(id) as user =>
      if (user.id != id) Forbidden() else request.req.decode[UserData] { form =>
        UsersService.update(id, form.name, form.email).foldM(errorMapper, _ => Ok())
      }
    case GET -> Root / LongVar(id) / "events" as user =>
      if (user.id != id) Forbidden()
      else ParticipationService.getEvents(user.id).foldM(errorMapper, Ok(_))
    case GET -> Root / LongVar(id) / "organizations" as user =>
      if (user.id != id) Forbidden()
      else ParticipationService.getOrganizations(user.id).foldM(errorMapper, Ok(_))
  }

  def routes(implicit middleware: AuthMiddleware[Task, User]): HttpRoutes[Task] = Router(
    prefixPath -> httpRoutes
  )
}
