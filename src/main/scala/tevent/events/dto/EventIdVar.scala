package tevent.events.dto

import tevent.events.model.EventId

object EventIdVar {
  def unapply(str: String): Option[EventId] =
    if (str.nonEmpty)
      str.toLongOption.map(EventId(_))
    else
      None
}
