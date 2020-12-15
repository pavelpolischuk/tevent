package tevent.user.model

import tevent.core.EntityType

case class User(id: Long,
                name: String,
                email: String,
                secretHash: String,
                lastRevoke: Long)

object User {
  implicit val userNamed: EntityType[User] = new EntityType[User] {
    override val name: String = "User"
  }

  def mapperTo(tuple: (Long, String, String, String, Long)): User =
    User(tuple._1, tuple._2, tuple._3, tuple._4, tuple._5)
}
