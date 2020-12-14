package tevent.http.endpoints

import cats.data.{Kleisli, OptionT}
import org.http4s._
import org.http4s.dsl.Http4sDsl
import org.http4s.headers.Authorization
import tevent.domain.model.User
import tevent.domain.{DomainError, ValidationError}
import tevent.http.model.user.{LoginData, LoginForm}
import tevent.infrastructure.service.Crypto
import tevent.service.AuthService
import zio.clock.Clock
import zio.interop.catz.taskConcurrentInstance
import zio.{RIO, ZIO}

final class AuthEndpoint[R <: AuthService with Crypto with Clock] {
  type Task[A] = RIO[R, A]

  private val dsl = Http4sDsl[Task]
  import dsl._

  def forbidden: AuthedRoutes[DomainError, Task] = {
    Kleisli(req => OptionT.liftF(Forbidden(req.context.message)))
  }

  def authUser: Kleisli[Task, Request[Task], Either[DomainError, User]] = Kleisli({ request =>
    ZIO.fromEither(request.headers.get(Authorization)
      .map(_.credentials)
      .flatMap {
        case Credentials.Token(scheme, token) if scheme == AuthScheme.Bearer => Some(token)
        case _ => None
      }
      .toRight(ValidationError("Couldn't find an Authorization Bearer header")))
      .flatMap(AuthService.validateUser).fold(f => Left(f), u => Right(u))
  })

  private val httpRoutes = HttpRoutes.of[Task] {
    case request@POST -> Root / "signin" => request.decode[LoginForm] { form =>
      AuthService.signIn(form.name.getOrElse(""), form.email, form.secret).foldM(
        failure = errorMapper,
        success = token => Ok(LoginData("Signed in", token.signedString))
      )
    }
    case request@POST -> Root / "login" => request.decode[LoginForm] { form =>
      AuthService.login(form.email, form.secret).foldM(
          failure = errorMapper,
          success = token => Ok(LoginData("Logged in", token.signedString))
        )
    }
  }

  def routes: HttpRoutes[Task] = httpRoutes
}
