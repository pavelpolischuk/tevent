package tevent.http.endpoints

import org.http4s.HttpRoutes
import org.http4s.dsl.Http4sDsl
import org.http4s.server.Router
import zio._
import zio.interop.catz.core._

final class HealthEndpoint[R] {
  type HealthTask[A] = RIO[R, A]

  private val prefixPath = "/health"
  private val dsl: Http4sDsl[HealthTask] = Http4sDsl[HealthTask]
  import dsl._

  private val httpRoutes = HttpRoutes.of[HealthTask] {
    case GET -> Root => Ok("OK")
  }

  val routes: HttpRoutes[HealthTask] = Router(
    prefixPath -> httpRoutes
  )
}