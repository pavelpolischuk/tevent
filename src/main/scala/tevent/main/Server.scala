package tevent.main

import cats.data.Kleisli
import cats.effect.ExitCode
import cats.implicits.toSemigroupKOps
import org.http4s.implicits.http4sKleisliResponseSyntaxOptionT
import org.http4s.server.blaze.BlazeServerBuilder
import org.http4s.server.middleware.{AutoSlash, GZip}
import org.http4s.server.{AuthMiddleware, Router}
import org.http4s.{HttpRoutes, Request, Response}
import tevent.core.Config
import tevent.events.EventsEndpoint
import tevent.main.Environments.AppEnvironment
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

  def createRoutes(root: String): ServerRoutes = {
    val auth = new AuthEndpoint[AppEnvironment]

    implicit val authMiddleware: AuthMiddleware[ServerRIO, User] = AuthMiddleware(auth.authUser, auth.forbidden)

    val users = new UserEndpoint[AppEnvironment]
    val events = new EventsEndpoint[AppEnvironment]
    val organizations = new OrganizationsEndpoint[AppEnvironment]
    val health = new HealthEndpoint[AppEnvironment]
    val docs = auth.docRoutes(root) ++ users.docRoutes(root) ++ organizations.docRoutes(root) ++ events.docRoutes(root)
    val routes = users.routes <+> events.routes <+> organizations.routes <+> auth.routes <+> health.routes(root, docs)

    Router[ServerRIO](root -> middleware(routes)).orNotFound
  }

  private val middleware: HttpRoutes[ServerRIO] => HttpRoutes[ServerRIO] = {
    { http: HttpRoutes[ServerRIO] =>
      AutoSlash(http)
    }.andThen { http: HttpRoutes[ServerRIO] =>
      GZip(http)
    }
  }
}
