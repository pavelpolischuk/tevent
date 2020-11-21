package tevent.http.endpoints

import org.http4s.HttpRoutes
import org.http4s.dsl.Http4sDsl
import org.http4s.server.Router
import zio._
import zio.interop.catz.core._

final class HealthEndpoint[R] {
  type Task[A] = RIO[R, A]

  private val prefixPath = "/health"
  private val dsl: Http4sDsl[Task] = Http4sDsl[Task]
  import dsl._

  private val httpRoutes = HttpRoutes.of[Task] {
    case GET -> Root => Ok("OK")
  }

  val routes: HttpRoutes[Task] = Router(
    prefixPath -> httpRoutes
  )
}
