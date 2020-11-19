package tevent.http.endpoints

import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}
import io.circe.{Decoder, Encoder}
import org.http4s.HttpRoutes
import org.http4s.dsl.Http4sDsl
import org.http4s.server.Router
import tevent.domain.model.Organization
import tevent.service.OrganizationsService
import zio._
import zio.interop.catz.taskConcurrentInstance

final class OrganizationsEndpoint[R <: OrganizationsService] {
  type OrganizationsTask[A] = RIO[R, A]

  private val prefixPath = "/organizations"
  private val dsl = Http4sDsl[OrganizationsTask]
  import dsl._

  implicit val orgEncoder: Encoder[Organization] = deriveEncoder[Organization]
  implicit val orgDecoder: Decoder[Organization] = deriveDecoder[Organization]

  private val httpRoutes = HttpRoutes.of[OrganizationsTask] {
    case GET -> Root / LongVar(id) => RIO.accessM[R].apply(
      _.get.get(id).foldM(errorMapper[OrganizationsTask],
        _.map(Ok(_)).getOrElse(NotFound())
      )
    )
  }

  val routes: HttpRoutes[OrganizationsTask] = Router(
    prefixPath -> httpRoutes
  )
}
