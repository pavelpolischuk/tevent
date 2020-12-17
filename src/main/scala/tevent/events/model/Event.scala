package tevent.events.model

import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}
import io.circe.{Decoder, Encoder}
import tevent.core.EntityType

import java.time.ZonedDateTime

case class Event(id: Long,
                 organizationId: Long,
                 name: String,
                 description: String,
                 datetime: ZonedDateTime,
                 location: Option[String],
                 capacity: Option[Int],
                 videoBroadcastLink: Option[String],
                 tags: List[String])

object Event {
  implicit val eventEncoder: Encoder[Event] = deriveEncoder[Event]
  implicit val eventDecoder: Decoder[Event] = deriveDecoder[Event]

  implicit object EventEntity extends EntityType[Event] {
    override val name: String = "Event"
  }

  def mapperTo(tuple: (Long, Long, String, String, ZonedDateTime, Option[String], Option[Int], Option[String], String)): Event =
    Event(tuple._1, tuple._2, tuple._3, tuple._4, tuple._5, tuple._6, tuple._7, tuple._8, tagsFromStr(tuple._9))

  def mapperFrom(event: Event): Option[(Long, Long, String, String, ZonedDateTime, Option[String], Option[Int], Option[String], String)] =
    Some((event.id, event.organizationId, event.name, event.description, event.datetime, event.location, event.capacity, event.videoBroadcastLink, tagsToStr(event.tags)))

  private[events] def tagsFromStr(tags: String): List[String] =
    tags.split(':').filter(_.nonEmpty).toList

  private[events] def tagsToStr(tags: List[String]): String =
    tags.mkString(":", ":", ":")
}
