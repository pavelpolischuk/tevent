package tevent.http

import io.circe.{Decoder, Encoder}
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}
import tevent.domain.model.{Event, Organization}

package object model {
  implicit val eventEncoder: Encoder[Event] = deriveEncoder[Event]
  implicit val eventDecoder: Decoder[Event] = deriveDecoder[Event]

  implicit val organizationEncoder: Encoder[Organization] = deriveEncoder[Organization]
  implicit val organizationDecoder: Decoder[Organization] = deriveDecoder[Organization]
}
