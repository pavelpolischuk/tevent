package tevent.domain

import tevent.domain.model._

trait EntityType[A] {
  val name: String
}

object Named {
  implicit val userNamed: EntityType[User] = new EntityType[User] {
    override val name: String = "User"
  }

  implicit val eventNamed: EntityType[Event] = new EntityType[Event] {
    override val name: String = "Event"
  }
  implicit val organizationNamed: EntityType[Organization] = new EntityType[Organization] {
    override val name: String = "Organization"
  }
}
