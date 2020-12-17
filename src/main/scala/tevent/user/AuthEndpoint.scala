package tevent.user

import cats.data.{Kleisli, OptionT}
import org.http4s._
import org.http4s.circe.CirceEntityCodec.{circeEntityDecoder, circeEntityEncoder}
import org.http4s.dsl.Http4sDsl
import org.http4s.headers.Authorization
import tevent.core.{DomainError, ValidationError}
import tevent.core.ErrorMapper.errorResponse
import tevent.user.dto.{LoginData, LoginForm}
import tevent.user.model.User
import tevent.user.service.{Auth, Crypto}
import zio.clock.Clock
import zio.interop.catz.taskConcurrentInstance
import zio.{RIO, ZIO}

final class AuthEndpoint[R <: Auth with Crypto with Clock] {
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
      .flatMap(Auth.validateUser).fold(f => Left(f), u => Right(u))
  })

  private val httpRoutes = HttpRoutes.of[Task] {
    case request@POST -> Root / "signin" => request.decode[LoginForm] { form =>
      Auth.signIn(form.name.getOrElse(""), form.email, form.secret).foldM(
        failure = errorResponse,
        success = token => Ok(LoginData("Signed in", token.signedString))
      )
    }
    case request@POST -> Root / "login" => request.decode[LoginForm] { form =>
      Auth.login(form.email, form.secret).foldM(
          failure = errorResponse,
          success = token => Ok(LoginData("Logged in", token.signedString))
        )
    }
  }

  def routes: HttpRoutes[Task] = httpRoutes
}