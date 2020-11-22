package tevent.http.endpoints

import org.http4s.dsl.Http4sDsl
import org.http4s.server.{AuthMiddleware, Router}
import org.http4s.{AuthedRoutes, HttpRoutes}
import tevent.domain.model.User
import tevent.http.model.OrganizationForm
import tevent.http.model.{eventEncoder, organizationEncoder}
import tevent.service.{EventsService, OrganizationsService}
import zio._
import zio.interop.catz.taskConcurrentInstance

final class OrganizationsEndpoint[R <: OrganizationsService with EventsService] {
  type Task[A] = RIO[R, A]

  private val prefixPath = "/organizations"
  private val dsl = Http4sDsl[Task]
  import dsl._

  private val httpRoutes = AuthedRoutes.of[User, Task] {
    case GET -> Root / LongVar(id) as user =>
      OrganizationsService.get(id).foldM(errorMapper, _.fold(NotFound())(Ok(_)))
    case GET -> Root / LongVar(id) / "events" as user =>
      EventsService.getByOrganization(id).foldM(errorMapper, Ok(_))
    case request@POST -> Root as user => request.req.decode[OrganizationForm] { form =>
      OrganizationsService.create(user.id, form.name).foldM(errorMapper, Ok(_))
    }
  }

  def routes(implicit middleware: AuthMiddleware[Task, User]): HttpRoutes[Task] = Router(
    prefixPath -> httpRoutes
  )
}
