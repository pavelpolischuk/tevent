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

  case class UserData(name: String, email: String)

  implicit val userEncoder: Encoder[UserData] = deriveEncoder[UserData]
  implicit val userDecoder: Decoder[UserData] = deriveDecoder[UserData]

  private val httpRoutes = AuthedRoutes.of[User, UsersTask] {
    case GET -> Root / LongVar(id) as user =>
      if (user.id == id) Ok(UserData(user.name, user.email)) else Forbidden()
    case request@PUT -> Root / LongVar(id) as user =>
      if (user.id != id) Forbidden() else request.req.decode[UserData] { form =>
        UsersService.update(id, form.name, form.email).foldM(errorMapper, _ => Ok())
      }
  }

  def routes(implicit middleware: AuthMiddleware[UsersTask, User]): HttpRoutes[UsersTask] = Router(
    prefixPath -> middleware(httpRoutes)
  )
}
