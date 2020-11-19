package tevent.http.endpoints

import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}
import io.circe.{Decoder, Encoder}
import org.http4s.circe.{jsonEncoderOf, jsonOf}
import org.http4s.dsl.Http4sDsl
import org.http4s.server.Router
import org.http4s.{EntityDecoder, EntityEncoder, HttpRoutes}
import tevent.domain.model.Event
import tevent.domain.{DomainError, ValidationError}
import tevent.service.EventsService
import zio._
import zio.interop.catz.taskConcurrentInstance

final class EventsEndpoint[R <: EventsService] {
  type EventsTask[A] = RIO[R, A]

  private val prefixPath = "/events"

  val dsl = Http4sDsl[EventsTask]
  import dsl._

  implicit val eventEncoder: Encoder[Event] = deriveEncoder[Event]
  implicit val eventDecoder: Decoder[Event] = deriveDecoder[Event]
  implicit def circeJsonDecoder[A: Decoder]: EntityDecoder[EventsTask, A] = jsonOf[EventsTask, A]
  implicit def circeJsonEncoder[A: Encoder]: EntityEncoder[EventsTask, A] = jsonEncoderOf[EventsTask, A]

  private val errorMapper = (e: DomainError) => e match {
    case ValidationError(m) => BadRequest(m)
    case _ => InternalServerError()
  }

  private val httpRoutes = HttpRoutes.of[EventsTask] {
    case GET -> Root / LongVar(id) => RIO.accessM[R].apply(
      _.get.get(id).foldM(errorMapper,
        _.map(Ok(_)).getOrElse(NotFound())
      )
    )
  }

  val routes: HttpRoutes[EventsTask] = Router(
    prefixPath -> httpRoutes
  )
}
