package tevent.http.endpoints

import org.http4s.dsl.Http4sDsl
import org.http4s.server.{AuthMiddleware, Router}
import org.http4s.{AuthedRoutes, HttpRoutes}
import tevent.domain.model.User
import tevent.http.model.EventForm
import tevent.http.model.EventParticipation.eventEncoder
import tevent.service.EventsService
import zio._
import zio.interop.catz.taskConcurrentInstance

final class EventsEndpoint[R <: EventsService] {
  type Task[A] = RIO[R, A]

  private val prefixPath = "/events"
  private val dsl = Http4sDsl[Task]
  import dsl._

  private val httpRoutes = AuthedRoutes.of[User, Task] {
    case GET -> Root / LongVar(id) as user =>
      EventsService.get(id).foldM(errorMapper, _.fold(NotFound())(Ok(_)))
    case request@POST -> Root as user => request.req.decode[EventForm] { form =>
      EventsService.create(user.id, form.organizationId, form.name, form.datetime, form.location, form.capacity, form.videoBroadcastLink)
        .foldM(errorMapper, Ok(_))
    }
  }

  def routes(implicit middleware: AuthMiddleware[Task, User]): HttpRoutes[Task] = Router(
    prefixPath -> httpRoutes
  )
}
