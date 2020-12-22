package tevent.events.dto

import io.circe.{Decoder, Encoder}
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}
import tevent.events.model.Event
import tevent.organizations.model.PlainOrganization

import java.time.ZonedDateTime

case class EventData(id: Long,
                     organization: PlainOrganization,
                     name: String,
                     description: String,
                     datetime: ZonedDateTime,
                     location: Option[String],
                     capacity: Option[Int],
                     videoBroadcastLink: Option[String],
                     tags: List[String])

object EventData {
  implicit val eventDataEncoder: Encoder[EventData] = deriveEncoder[EventData]
  implicit val eventDataDecoder: Decoder[EventData] = deriveDecoder[EventData]

  def apply(tuple: (Event, PlainOrganization)): EventData =
    EventData(tuple._1.id, tuple._2, tuple._1.name, tuple._1.description, tuple._1.datetime,
      tuple._1.location, tuple._1.capacity, tuple._1.videoBroadcastLink, tuple._1.tags)
}
