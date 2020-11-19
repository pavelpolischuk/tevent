package tevent.http.endpoints

import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}
import io.circe.{Decoder, Encoder}
import org.http4s.circe.{jsonEncoderOf, jsonOf}
import org.http4s.dsl.Http4sDsl
import org.http4s.server.Router
import org.http4s.{EntityDecoder, EntityEncoder, HttpRoutes}
import tevent.domain.model.Organization
import tevent.domain.{DomainError, ValidationError}
import tevent.service.OrganizationsService
import zio._
import zio.interop.catz.taskConcurrentInstance

final class OrganizationsEndpoint[R <: OrganizationsService] {
  type OrganizationsTask[A] = RIO[R, A]

  private val prefixPath = "/organizations"

  val dsl = Http4sDsl[OrganizationsTask]
  import dsl._

  implicit val orgEncoder: Encoder[Organization] = deriveEncoder[Organization]
  implicit val orgDecoder: Decoder[Organization] = deriveDecoder[Organization]
  implicit def circeJsonDecoder[A: Decoder]: EntityDecoder[OrganizationsTask, A] = jsonOf[OrganizationsTask, A]
  implicit def circeJsonEncoder[A: Encoder]: EntityEncoder[OrganizationsTask, A] = jsonEncoderOf[OrganizationsTask, A]

  private val errorMapper = (e: DomainError) => e match {
    case ValidationError(m) => BadRequest(m)
    case _ => InternalServerError()
  }

  private val httpRoutes = HttpRoutes.of[OrganizationsTask] {
    case GET -> Root / LongVar(id) => RIO.accessM[R].apply(
      _.get.get(id).foldM(errorMapper,
        _.map(Ok(_)).getOrElse(NotFound())
      )
    )
  }

  val routes: HttpRoutes[OrganizationsTask] = Router(
    prefixPath -> httpRoutes
  )
}
