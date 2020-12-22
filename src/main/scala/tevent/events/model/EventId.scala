package tevent.events.model

import tevent.core.EntityType

case class EventId(id: Long)

object EventId {
  implicit object EventEntity extends EntityType[EventId] {
    override val name: String = "Event"
  }
}
