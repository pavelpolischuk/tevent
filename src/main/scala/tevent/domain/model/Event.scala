package tevent.domain.model

import io.circe.{Decoder, Encoder}
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}

import java.time.ZonedDateTime

case class Event(id: Long,
                 organizationId: Long,
                 name: String,
                 datetime: ZonedDateTime,
                 location: Option[String],
                 capacity: Option[Int],
                 videoBroadcastLink: Option[String])

object Event {
  implicit val eventEncoder: Encoder[Event] = deriveEncoder[Event]
  implicit val eventDecoder: Decoder[Event] = deriveDecoder[Event]

  def mapperTo(tuple: (Long, Long, String, ZonedDateTime, Option[String], Option[Int], Option[String])): Event =
    Event(tuple._1, tuple._2, tuple._3, tuple._4, tuple._5, tuple._6, tuple._7)
}
