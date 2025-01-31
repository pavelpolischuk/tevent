package tevent.events.dto

import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}
import io.circe.{Decoder, Encoder}

import java.time.ZonedDateTime

case class EventForm(organizationId: Long,
                     name: String,
                     description: String,
                     datetime: ZonedDateTime,
                     location: Option[String],
                     capacity: Option[Int],
                     videoBroadcastLink: Option[String],
                     tags: List[String])

object EventForm {
  implicit val eventFormDecoder: Decoder[EventForm] = deriveDecoder[EventForm]
  implicit val eventFormEncoder: Encoder[EventForm] = deriveEncoder[EventForm]
}
