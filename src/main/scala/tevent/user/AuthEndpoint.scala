package tevent.user

import cats.data.{Kleisli, OptionT}
import org.http4s._
import org.http4s.circe.CirceEntityCodec.{circeEntityDecoder, circeEntityEncoder}
import org.http4s.dsl.Http4sDsl
import org.http4s.headers.Authorization
import sttp.tapir._
import sttp.tapir.json.circe._
import sttp.tapir.generic.auto._
import tevent.core.ErrorMapper.errorResponse
import tevent.core.{DomainError, ValidationError}
import tevent.user.dto.LoginForm.TokenQueryParamMatcher
import tevent.user.dto.{GoogleLoginForm, LoginData, LoginForm}
import tevent.user.model.User
import tevent.user.service.Auth
import zio.interop.catz.taskConcurrentInstance
import zio.{RIO, ZIO}

final class AuthEndpoint[R <: Auth] {
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
    case request@POST -> Root / "auth" / "google" => request.decode[GoogleLoginForm] { form =>
      Auth.google(form.idtoken).foldM(
        failure = errorResponse,
        success = token => Ok(LoginData("Logged in", token.signedString))
      )
    }
    case GET -> Root / "auth" / "validate" :? TokenQueryParamMatcher(token) =>
      Auth.validateUser(token).foldM(
        failure = errorResponse,
        success = _ => Ok("OK")
      )
  }

  def routes: HttpRoutes[Task] = httpRoutes

  def docRoutes(basePath: String): List[Endpoint[_, _, _, _]] =  {

    val postSignin = endpoint.post
      .in(basePath / "signin")
      .in(jsonBody[LoginForm])
      .out(jsonBody[LoginData].example(LoginData("Signed in", "token-value")))

    val postLogin = endpoint.post
      .in(basePath / "login")
      .in(jsonBody[LoginForm])
      .out(jsonBody[LoginData].example(LoginData("Logged in", "token-value")))

    val postGoogle = endpoint.post
      .in(basePath / "auth" / "google")
      .in(jsonBody[GoogleLoginForm])
      .out(jsonBody[LoginData].example(LoginData("Logged in", "token-value")))

    val getValidate = endpoint.get
      .in(basePath / "auth" / "validate")
      .in(query[String]("token").description("Token to validate"))
      .out(stringBody.example("OK"))

    List(postSignin, postLogin, postGoogle, getValidate)
  }
}
