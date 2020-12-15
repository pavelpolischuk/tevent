package tevent.main.health

import org.http4s.HttpRoutes
import org.http4s.circe.CirceEntityCodec.circeEntityEncoder
import org.http4s.dsl.Http4sDsl
import zio._
import zio.interop.catz.core._

final class HealthEndpoint[R] {
  type Task[A] = RIO[R, A]

  private val dsl: Http4sDsl[Task] = Http4sDsl[Task]
  import dsl._

  private val httpRoutes = HttpRoutes.of[Task] {
    case GET -> Root / "health" => Ok("OK")
    case GET -> Root => Ok(List(
      Route(Method.GET, "/health"),
      Route(Method.POST, "/signin"),
      Route(Method.POST, "/login"),

      Route(Method.GET, "/user"),
      Route(Method.GET, "/user/events"),
      Route(Method.GET, "/user/organizations"),
      Route(Method.GET, "/user/requests"),
      Route(Method.POST, "/user/revoke"),
      Route(Method.PUT, "/user"),
      Route(Method.PUT, "/user/secret"),

      Route(Method.POST, "/organizations"),
      Route(Method.PUT, "/organizations/<id>"),
      Route(Method.POST, "/organizations/<id>/join"),
      Route(Method.POST, "/organizations/<id>/invite"),
      Route(Method.POST, "/organizations/<id>/approve"),
      Route(Method.POST, "/organizations/<id>/leave"),
      Route(Method.GET, "/organizations?tags=<tag1+tag2>"),
      Route(Method.GET, "/organizations/<id>"),
      Route(Method.GET, "/organizations/<id>/requests"),
      Route(Method.GET, "/organizations/<id>/users"),

      Route(Method.POST, "/events"),
      Route(Method.POST, "/events/<id>/join"),
      Route(Method.POST, "/events/<id>/leave"),
      Route(Method.GET, "/events?organization=<oId>&fromDate=<fd>&toDate=<td>&location=<loc>"),
      Route(Method.GET, "/events/<id>"),
      Route(Method.GET, "/events/<id>/users")
    ))
  }

  val routes: HttpRoutes[Task] = httpRoutes
}
