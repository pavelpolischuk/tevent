package tevent.events

import cats.implicits.toSemigroupKOps
import org.http4s.circe.CirceEntityCodec.{circeEntityDecoder, circeEntityEncoder}
import org.http4s.dsl.Http4sDsl
import org.http4s.server.{AuthMiddleware, Router}
import org.http4s.{AuthedRoutes, HttpRoutes}
import tevent.core.ErrorMapper.errorResponse
import tevent.events.dto.EventFilters._
import tevent.events.dto.{EventForm, EventParticipationForm, EventUserParticipationData}
import tevent.events.model.{Event, EventFilter, EventParticipation}
import tevent.events.service.{EventParticipants, Events}
import tevent.user.model.User
import zio._
import zio.interop.catz.taskConcurrentInstance

final class EventsEndpoint[R <: Services] {
  type Task[A] = RIO[R, A]

  private val dsl = Http4sDsl[Task]
  import dsl._

  private val httpRoutes = HttpRoutes.of[Task] {
    case GET -> Root / LongVar(id) =>
      Events.get(id).foldM(errorResponse, Ok(_))

    case GET -> Root
      :? OptionalFromDateQueryParamMatcher(fromDate)
      +& OptionalToDateQueryParamMatcher(toDate)
      +& OptionalOrganizationIdQueryParamMatcher(organizationId)
      +& OptionalLocationQueryParamMatcher(location)
      +& OptionalTagsQueryParamMatcher(tags) =>
      (fromDate.map(_.toEither), toDate.map(_.toEither), organizationId.map(_.toEither), tags.map(_.toEither)) match {
        case (Some(Left(_)), _, _, _) => BadRequest("unable to parse argument fromDate")
        case (_, Some(Left(_)), _, _) => BadRequest("unable to parse argument toDate")
        case (_, _, Some(Left(_)), _) => BadRequest("unable to parse argument organization")
        case (_, _, _, Some(Left(_))) => BadRequest("unable to parse argument tags")
        case _ =>
          val filter = EventFilter(organizationId.flatMap(_.toOption), fromDate.flatMap(_.toOption), toDate.flatMap(_.toOption), location, tags.flatMap(_.toOption).getOrElse(Array.empty))
          Events.search(filter).foldM(errorResponse, Ok(_))
      }
  }

  private val authedRoutes = AuthedRoutes.of[User, Task] {
    case request@POST -> Root as user => request.req.decode[EventForm] { form =>
      val event = Event(-1, form.organizationId, form.name, form.description, form.datetime, form.location, form.capacity, form.videoBroadcastLink, form.tags)
      Events.create(user.id, event).foldM(errorResponse, Ok(_))
    }

    case GET -> Root / LongVar(id) / "users" as user =>
      EventParticipants.getUsers(user.id, id).foldM(errorResponse,
        r => Ok(r.map(EventUserParticipationData.mapperTo)))
    case request@POST -> Root / LongVar(id) / "join" as user => request.req.decode[EventParticipationForm] { form =>
      val r = EventParticipation(user.id, id, form.participationType)
      EventParticipants.joinEvent(r).foldM(errorResponse, Ok(_))
    }
    case POST -> Root / LongVar(id) / "leave" as user =>
      EventParticipants.leaveEvent(user.id, id).foldM(errorResponse, Ok(_))
  }

  def routes(implicit middleware: AuthMiddleware[Task, User]): HttpRoutes[Task] =
    Router("/events" -> (httpRoutes <+> middleware(authedRoutes)))
}
