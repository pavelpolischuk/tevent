package tevent.http

import cats.Applicative
import cats.effect.Sync
import io.circe.{Decoder, Encoder}
import org.http4s.circe.{jsonEncoderOf, jsonOf}
import org.http4s.{EntityDecoder, EntityEncoder, Response}
import org.http4s.dsl.Http4sDsl
import tevent.domain.{DomainError, ValidationError}

package object endpoints {
  implicit def circeJsonDecoder[F[_]: Sync, A: Decoder]: EntityDecoder[F, A] = jsonOf[F, A]
  implicit def circeJsonEncoder[F[_]: Applicative, A: Encoder]: EntityEncoder[F, A] = jsonEncoderOf[F, A]

  def errorMapper[F[_]: Applicative]: DomainError => F[Response[F]] = {
    val dsl = Http4sDsl[F]
    import dsl._

    {
      case ValidationError(m) => BadRequest(m)
      case _ => InternalServerError()
    }
  }
}
