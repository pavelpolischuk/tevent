package tevent.events.model

import java.time.ZonedDateTime

case class EventFilter(organizationId: Option[Long],
                       fromDate: Option[ZonedDateTime],
                       toDate: Option[ZonedDateTime],
                       location: Option[String])
