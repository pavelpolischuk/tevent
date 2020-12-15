package tevent.events.dto

import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}
import io.circe.{Decoder, Encoder}
import tevent.events.model.EventParticipationType

case class EventParticipationForm(participationType: EventParticipationType)

object EventParticipationForm {
  implicit val eventFormEncoder: Encoder[EventParticipationForm] = deriveEncoder[EventParticipationForm]
  implicit val eventFormDecoder: Decoder[EventParticipationForm] = deriveDecoder[EventParticipationForm]
}
