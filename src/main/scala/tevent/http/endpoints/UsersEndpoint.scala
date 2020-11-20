package tevent.http.endpoints

import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}
import io.circe.{Decoder, Encoder}
import org.http4s.dsl.Http4sDsl
import org.http4s.server.{AuthMiddleware, Router}
import org.http4s.{AuthedRoutes, HttpRoutes}
import tevent.domain.model.User
import tevent.service.UsersService
import zio._
import zio.interop.catz.taskConcurrentInstance

final class UsersEndpoint[R <: UsersService] {
  type UsersTask[A] = RIO[R, A]

  private val prefixPath = "/users"
  private val dsl = Http4sDsl[UsersTask]
  import dsl._

  implicit val userEncoder: Encoder[User] = deriveEncoder[User]
  implicit val userDecoder: Decoder[User] = deriveDecoder[User]

  private val httpRoutes = AuthedRoutes.of[User, UsersTask] {
    case GET -> Root / LongVar(id) as user => RIO.accessM[R].apply(
      _.get.get(id).foldM(errorMapper, Ok(_))
    )
  }

  def routes(implicit middleware: AuthMiddleware[UsersTask, User]): HttpRoutes[UsersTask] = Router(
    prefixPath -> middleware(httpRoutes)
  )
}
