package tevent.http.model

import io.circe.Decoder
import io.circe.generic.semiauto.deriveDecoder

import java.time.ZonedDateTime

case class EventForm(organizationId: Long,
                     name: String,
                     datetime: ZonedDateTime,
                     location: Option[String],
                     capacity: Option[Int],
                     videoBroadcastLink: Option[String])

object EventForm {
  implicit val eventFormDecoder: Decoder[EventForm] = deriveDecoder[EventForm]
}
