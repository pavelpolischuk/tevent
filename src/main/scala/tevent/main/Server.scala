package tevent.main

import cats.data.Kleisli
import cats.effect.ExitCode
import org.http4s.server.blaze.BlazeServerBuilder
import org.http4s.server.middleware.{AutoSlash, GZip}
import org.http4s.server.{AuthMiddleware, Router}
import org.http4s.{HttpRoutes, Request, Response}
import tevent.core.Config
import tevent.events.EventsEndpoint
import Environments.AppEnvironment
import cats.implicits.toSemigroupKOps
import org.http4s.implicits.http4sKleisliResponseSyntaxOptionT
import tevent.main.health.HealthEndpoint
import tevent.organizations.OrganizationsEndpoint
import tevent.user.model.User
import tevent.user.{AuthEndpoint, UserEndpoint}
import zio.interop.catz.{taskConcurrentInstance, taskEffectInstance, zioTimer}
import zio.{RIO, ZIO}

object Server {
  type ServerRIO[A] = RIO[AppEnvironment, A]
  type ServerRoutes = Kleisli[ServerRIO, Request[ServerRIO], Response[ServerRIO]]

  def runServer: ZIO[AppEnvironment, Nothing, Unit] =
    ZIO.runtime[AppEnvironment].flatMap { implicit rts =>
      val cfg = rts.environment.get[Config.HttpServerConfig]
      val ec = rts.platform.executor.asEC

      BlazeServerBuilder[ServerRIO](ec)
        .bindHttp(cfg.port, cfg.host)
        .withHttpApp(createRoutes(cfg.path))
        .serve
        .compile[ServerRIO, ServerRIO, ExitCode]
        .drain
    }
      .orDie

  def createRoutes(basePath: String): ServerRoutes = {
    val authEndpoint = new AuthEndpoint[AppEnvironment]

    implicit val auth: AuthMiddleware[ServerRIO, User] = AuthMiddleware(authEndpoint.authUser, authEndpoint.forbidden)

    val usersRoutes = new UserEndpoint[AppEnvironment].routes
    val eventsRoutes = new EventsEndpoint[AppEnvironment].routes
    val organizationsRoutes = new OrganizationsEndpoint[AppEnvironment].routes
    val healthRoutes = new HealthEndpoint[AppEnvironment].routes
    val authRoutes = authEndpoint.routes
    val routes = usersRoutes <+> eventsRoutes <+> organizationsRoutes <+> healthRoutes <+> authRoutes

    Router[ServerRIO](basePath -> middleware(routes)).orNotFound
  }

  private val middleware: HttpRoutes[ServerRIO] => HttpRoutes[ServerRIO] = {
    { http: HttpRoutes[ServerRIO] =>
      AutoSlash(http)
    }.andThen { http: HttpRoutes[ServerRIO] =>
      GZip(http)
    }
  }
}
