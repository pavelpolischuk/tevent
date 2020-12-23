package tevent.main.health

import cats.implicits.toSemigroupKOps
import org.http4s.HttpRoutes
import org.http4s.circe.CirceEntityCodec.circeEntityEncoder
import org.http4s.dsl.Http4sDsl
import sttp.tapir._
import sttp.tapir.docs.openapi.OpenAPIDocsInterpreter
import sttp.tapir.openapi.OpenAPI
import sttp.tapir.openapi.circe.yaml.RichOpenAPI
import sttp.tapir.swagger.http4s.SwaggerHttp4s
import zio._
import zio.interop.catz.{taskConcurrentInstance, zioContextShift}

final class HealthEndpoint[R] {
  type Task[A] = RIO[R, A]

  private val dsl: Http4sDsl[Task] = Http4sDsl[Task]
  import dsl._

  private val httpRoutes = HttpRoutes.of[Task] {
    case GET -> Root / "health" => Ok("OK")
  }

  def routes(basePath: String, elseEndpoints: Seq[Endpoint[_, _, _, _]]): HttpRoutes[Task] = {

    val getHealth = endpoint.get
      .in(basePath / "health")
      .out(stringBody.example("OK"))

    val openApiDocs: OpenAPI = OpenAPIDocsInterpreter.toOpenAPI(elseEndpoints :+ getHealth, "The events web api", "1.0.0")
    val openApiYml: String = openApiDocs.toYaml
    val openApi = new SwaggerHttp4s(openApiYml)

    httpRoutes <+> openApi.routes[Task]
  }
}
