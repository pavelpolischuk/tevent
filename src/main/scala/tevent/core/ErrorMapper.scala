package tevent.core

import cats.Applicative
import org.http4s.Response
import org.http4s.dsl.Http4sDsl

object ErrorMapper {
  def errorResponse[F[_]: Applicative]: DomainError => F[Response[F]] = {
    val dsl = Http4sDsl[F]
    import dsl._

    {
      case er@EntityNotFound(_) => NotFound(er.message)
      case ValidationError(m) => BadRequest(m)
      case AccessError => Forbidden()
      case er => InternalServerError(er.message)
    }
  }
}
