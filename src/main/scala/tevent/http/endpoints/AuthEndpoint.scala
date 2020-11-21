package tevent.http.endpoints

import cats.data.{Kleisli, OptionT}
import io.circe.{Decoder, Encoder}
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}
import org.http4s._
import org.http4s.dsl.Http4sDsl
import org.http4s.server.Router
import tevent.domain.model.User
import tevent.domain.{DomainError, ValidationError}
import tevent.infrastructure.service.Crypto
import tevent.service.AuthService
import zio.clock.Clock
import zio.interop.catz.taskConcurrentInstance
import zio.{RIO, ZIO}

final class AuthEndpoint[R <: AuthService with Crypto with Clock] {
  type Task[A] = RIO[R, A]

  private val prefixPath = "/"
  private val dsl = Http4sDsl[Task]
  import dsl._

  case class LoginForm(name: Option[String], email: String, secret: String)
  case class LoginData(message: String, token: String)

  implicit val loginDecoder: Decoder[LoginForm] = deriveDecoder[LoginForm]
  implicit val loginEncoder: Encoder[LoginData] = deriveEncoder[LoginData]

  def forbidden: AuthedRoutes[DomainError, Task] = {
    Kleisli(req => OptionT.liftF(Forbidden(req.context.message)))
  }

  def authUser: Kleisli[Task, Request[Task], Either[DomainError, User]] = Kleisli({ request =>
    val value = for {
      header <- ZIO.fromEither(headers.Cookie.from(request.headers).toRight(ValidationError("Cookie parsing error")))
      cookie <- ZIO.fromEither(header.values.toList.find(_.name == "authcookie").toRight(ValidationError("Couldn't find the authcookie")))
      res <- AuthService.validateUser(cookie.content)
    } yield res
    value.fold(f => Left(f), u => Right(u))
  })

  private val httpRoutes = HttpRoutes.of[Task] {
    case request@POST -> Root / "signin" => request.decode[LoginForm] { form =>
      AuthService.signIn(form.name.getOrElse(""), form.email, form.secret).foldM(
        failure = errorMapper,
        success = token => Ok(LoginData("Signed in", token)).map(_.addCookie(ResponseCookie("authcookie", token)))
      )
    }
    case request@POST -> Root / "login" => request.decode[LoginForm] { form =>
      AuthService.login(form.email, form.secret).foldM(
          failure = errorMapper,
          success = token => Ok(LoginData("Logged in", token)).map(_.addCookie(ResponseCookie("authcookie", token)))
        )
    }
  }

  def routes: HttpRoutes[Task] = Router(
    prefixPath -> httpRoutes
  )
}
