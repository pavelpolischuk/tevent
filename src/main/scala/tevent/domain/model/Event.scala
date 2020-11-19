package tevent.domain.model

import java.time.ZonedDateTime

case class Event(id: Option[Long],
                 organizationId: Long,
                 name: String,
                 datetime: ZonedDateTime,
                 location: Option[String],
                 capacity: Option[Int],
                 videoBroadcastLink: Option[String])
