package tevent.http

import cats.data.Kleisli
import cats.effect.ExitCode
import cats.implicits._
import org.http4s.implicits._
import org.http4s.server.blaze.BlazeServerBuilder
import org.http4s.server.middleware.{AutoSlash, GZip}
import org.http4s.server.{AuthMiddleware, Router}
import org.http4s.{HttpRoutes, Request, Response}
import tevent.domain.model.User
import tevent.http.endpoints.{AuthEndpoint, EventsEndpoint, HealthEndpoint, OrganizationsEndpoint, UserEndpoint}
import tevent.infrastructure.Configuration
import tevent.infrastructure.Environments.AppEnvironment
import zio.interop.catz._
import zio.{RIO, ZIO}

object Server {
  type ServerRIO[A] = RIO[AppEnvironment, A]
  type ServerRoutes = Kleisli[ServerRIO, Request[ServerRIO], Response[ServerRIO]]

  def runServer: ZIO[AppEnvironment, Nothing, Unit] =
    ZIO.runtime[AppEnvironment].flatMap { implicit rts =>
      val cfg = rts.environment.get[Configuration.HttpServerConfig]
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
    val routes = authEndpoint.routes <+> usersRoutes <+> healthRoutes <+> organizationsRoutes <+> eventsRoutes

    Router[ServerRIO](basePath -> middleware(routes)).orNotFound
  }

  private val middleware: HttpRoutes[ServerRIO] => HttpRoutes[ServerRIO] = {
    { http: HttpRoutes[ServerRIO] =>
      AutoSlash(http)
    }.andThen { http: HttpRoutes[ServerRIO] =>
      GZip(http)
    }
  }

//  def stream[F[_] : ConcurrentEffect](implicit T: Timer[F], C: ContextShift[F]): Stream[F, Nothing] = {
//    for {
//      client <- BlazeClientBuilder[F](global).stream
//      helloWorldAlg = HelloWorld.impl[F]
//      jokeAlg = Jokes.impl[F](client)
//
//      httpApp = (
//        TeventRoutes.helloWorldRoutes[F](helloWorldAlg) <+>
//          TeventRoutes.jokeRoutes[F](jokeAlg)
//        ).orNotFound
//
//      // With Middlewares in place
//      finalHttpApp = Logger.httpApp(logHeaders = true, logBody = true)(httpApp)
//
//      exitCode <- BlazeServerBuilder[F](global)
//        .bindHttp(8080, "0.0.0.0")
//        .withHttpApp(finalHttpApp)
//        .serve
//    } yield exitCode
//  }.drain
}
