package tevent.http.endpoints

import cats.implicits.toSemigroupKOps
import org.http4s.dsl.Http4sDsl
import org.http4s.server.{AuthMiddleware, Router}
import org.http4s.{AuthedRoutes, HttpRoutes}
import tevent.domain.model.{Event, EventFilter, EventParticipation, User}
import tevent.http.model.event.EventFilters._
import tevent.http.model.event.{EventForm, EventParticipationForm, EventUserParticipationData}
import tevent.service.EventsService
import zio._
import zio.interop.catz.taskConcurrentInstance

final class EventsEndpoint[R <: EventsService] {
  type Task[A] = RIO[R, A]

  private val dsl = Http4sDsl[Task]
  import dsl._

  private val httpRoutes = HttpRoutes.of[Task] {
    case GET -> Root / LongVar(id) =>
      EventsService.get(id).foldM(errorMapper, _.fold(NotFound())(Ok(_)))

    case GET -> Root
      :? OptionalFromDateQueryParamMatcher(fromDate)
      +& OptionalToDateQueryParamMatcher(toDate)
      +& OptionalOrganizationIdQueryParamMatcher(organizationId)
      +& OptionalLocationQueryParamMatcher(location) =>
      (fromDate.map(_.toEither), toDate.map(_.toEither), organizationId.map(_.toEither)) match {
        case (Some(Left(_)), _, _) => BadRequest("unable to parse argument fromDate")
        case (_, Some(Left(_)), _) => BadRequest("unable to parse argument toDate")
        case (_, _, Some(Left(_))) => BadRequest("unable to parse argument organization")
        case _ =>
          val filter = EventFilter(organizationId.flatMap(_.toOption), fromDate.flatMap(_.toOption), toDate.flatMap(_.toOption), location)
          EventsService.search(filter).foldM(errorMapper, Ok(_))
      }
  }

  private val authedRoutes = AuthedRoutes.of[User, Task] {
    case request@POST -> Root as user => request.req.decode[EventForm] { form =>
      val event = Event(-1, form.organizationId, form.name, form.nick, form.datetime, form.location, form.capacity, form.videoBroadcastLink)
      EventsService.create(user.id, event).foldM(errorMapper, Ok(_))
    }
    case GET -> Root / LongVar(id) / "users" as user =>
      EventsService.getUsers(user.id, id).foldM(errorMapper,
        r => Ok(r.map(EventUserParticipationData.mapperTo)))

    case request@POST -> Root / LongVar(id) / "join" as user => request.req.decode[EventParticipationForm] { form =>
      val r = EventParticipation(user.id, id, form.participationType)
      EventsService.joinEvent(r).foldM(errorMapper, Ok(_))
    }
    case POST -> Root / LongVar(id) / "leave" as user =>
      EventsService.leaveEvent(user.id, id).foldM(errorMapper, Ok(_))
  }

  def routes(implicit middleware: AuthMiddleware[Task, User]): HttpRoutes[Task] =
    Router("/events" -> (httpRoutes <+> middleware(authedRoutes)))
}
