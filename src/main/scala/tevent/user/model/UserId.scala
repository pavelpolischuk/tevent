package tevent.user.model

import tevent.core.EntityType

case class UserId(id: Long)

object UserId {
  implicit object UserEntity extends EntityType[UserId] {
    override val name: String = "User"
  }
}
