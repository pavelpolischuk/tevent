package tevent.events

import cats.implicits.toSemigroupKOps
import org.http4s.circe.CirceEntityCodec.{circeEntityDecoder, circeEntityEncoder}
import org.http4s.dsl.Http4sDsl
import org.http4s.server.{AuthMiddleware, Router}
import org.http4s.{AuthedRoutes, HttpRoutes}
import sttp.tapir.EndpointIO.Example
import sttp.tapir._
import sttp.tapir.json.circe._
import sttp.tapir.generic.auto._
import tevent.core.ErrorMapper.errorResponse
import tevent.events.dto.EventFilters._
import tevent.events.dto.{EventData, EventForm, EventIdVar, EventParticipationForm, EventUserParticipationData}
import tevent.events.model.{Event, EventFilter, EventParticipation, EventSubscriber, OfflineParticipant, OnlineParticipant}
import tevent.events.service.{EventParticipants, Events}
import tevent.user.model.User
import zio._
import zio.interop.catz.taskConcurrentInstance

import java.time.{ZoneOffset, ZonedDateTime}

final class EventsEndpoint[R <: Services] {
  type Task[A] = RIO[R, A]

  private val dsl = Http4sDsl[Task]
  import dsl._

  private val httpRoutes = HttpRoutes.of[Task] {
    case GET -> Root / EventIdVar(id) =>
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
      Events.create(user.typedId, event).foldM(errorResponse, Ok(_))
    }
    case request@PUT -> Root / EventIdVar(id) as user => request.req.decode[EventForm] { form =>
      val event = Event(id.id, form.organizationId, form.name, form.description, form.datetime, form.location, form.capacity, form.videoBroadcastLink, form.tags)
      Events.update(user.typedId, event).foldM(errorResponse, Ok(_))
    }

    case GET -> Root / EventIdVar(id) / "users" as user =>
      EventParticipants.getUsers(user.typedId, id).foldM(errorResponse,
        r => Ok(r.map(EventUserParticipationData.mapperTo)))
    case request@POST -> Root / EventIdVar(id) / "join" as user => request.req.decode[EventParticipationForm] { form =>
      val r = EventParticipation(user.id, id.id, form.participationType)
      EventParticipants.joinEvent(r).foldM(errorResponse, Ok(_))
    }
    case POST -> Root / EventIdVar(id) / "leave" as user =>
      EventParticipants.leaveEvent(user.typedId, id).foldM(errorResponse, Ok(_))
  }

  def routes(implicit middleware: AuthMiddleware[Task, User]): HttpRoutes[Task] =
    Router("/events" -> (httpRoutes <+> middleware(authedRoutes)))

  def docRoutes(basePath: String): List[Endpoint[_, _, _, _]] = {

    val eventId = path[Long]("eventId")
    val dateSample = ZonedDateTime.of(1991, 12, 26, 12, 0, 0, 0, ZoneOffset.UTC)

    val eventForm = jsonBody[EventForm]
      .description("The event data")
      .example(EventForm(42, "Event #1", "Event description", dateSample, Some("Moscow"), Some(128), None, List("tag1", "tag2")))

    val authHeader = auth.bearer[String]()

    val getEvent = endpoint.get
      .in(basePath / "events")
      .in(eventId)
      .out(jsonBody[EventData])

    val getEventList = endpoint.get
      .in(basePath / "events")
      .in(
        query[Option[String]]("tags")
          .description("Tags to search")
          .example(Some("it+dev+scala")).and(
          query[Option[ZonedDateTime]]("fromDate")
            .description("Start datetime to search")
            .example(Some(dateSample))).and(
          query[Option[ZonedDateTime]]("toDate")
            .description("End datetime to search")
            .example(Some(dateSample))).and(
          query[Option[Long]]("organizationId")
            .description("Organization ID to search")
            .example(Some(42L))).and(
          query[Option[String]]("location")
            .description("End datetime to search")
            .example(Some("Moscow")))
      )
      .out(jsonBody[List[EventData]])


    val postEvent = endpoint.post
      .in(basePath / "events")
      .in(authHeader)
      .in(eventForm)
      .out(jsonBody[Event])

    val putEvent = endpoint.put
      .in(basePath / "events")
      .in(eventId)
      .in(authHeader)
      .in(eventForm)

    val getUsers = endpoint.get
      .in(basePath / "events")
      .in(eventId)
      .in("users")
      .in(authHeader)
      .out(jsonBody[List[EventUserParticipationData]])

    val postJoin = endpoint.post
      .in(basePath / "events")
      .in(eventId)
      .in("join")
      .in(authHeader)
      .in(jsonBody[EventParticipationForm]
        .examples(List(EventSubscriber, OnlineParticipant, OfflineParticipant).map(t => Example.of(EventParticipationForm(t), Some(t.toString)))))

    val postLeave = endpoint.post
      .in(basePath / "events")
      .in(eventId)
      .in("leave")
      .in(authHeader)

    List(getEvent, getEventList, postEvent, putEvent, getUsers, postJoin, postLeave)
  }
}
