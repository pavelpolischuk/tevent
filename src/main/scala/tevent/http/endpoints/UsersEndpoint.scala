package tevent.http.endpoints

import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}
import io.circe.{Decoder, Encoder}
import org.http4s.HttpRoutes
import org.http4s.dsl.Http4sDsl
import org.http4s.server.Router
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

  private val httpRoutes = HttpRoutes.of[UsersTask] {
    case GET -> Root / LongVar(id) => RIO.accessM[R].apply(
      _.get.get(id).foldM(errorMapper,
        _.map(Ok(_)).getOrElse(NotFound())
      )
    )
    case request@POST -> Root / "signin" => RIO.accessM[R].apply( repo =>
      request.decode[User] { user =>
        repo.get.signIn(user.name, user.email, user.secretHash).foldM(
          failure = errorMapper,
          success = Ok(_)
        )
      }
    )
  }

  val routes: HttpRoutes[UsersTask] = Router(
    prefixPath -> httpRoutes
  )
}
