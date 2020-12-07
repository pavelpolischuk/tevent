package tevent.http.model

import io.circe.Encoder
import io.circe.generic.semiauto.deriveEncoder

sealed trait Method
object Method {
  object GET extends Method
  object POST extends Method
  object PUT extends Method
}

case class Route(method: Method, path: String)

object Route {
  implicit val httpMethodEncoder: Encoder[Method] = Encoder[String].contramap {
    case Method.GET => "GET"
    case Method.POST => "POST"
    case Method.PUT => "PUT"
  }

  implicit val routeEncoder: Encoder[Route] = deriveEncoder[Route]
}
