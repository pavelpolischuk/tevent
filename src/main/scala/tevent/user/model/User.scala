package tevent.user.model

case class User(id: Long,
                name: String,
                email: String,
                secretHash: Option[String],
                googleId: Option[String],
                lastRevoke: Long) {

  def typedId: UserId = UserId(id)
}

object User {
  def mapperTo(tuple: (Long, String, String, Option[String], Option[String], Long)): User =
    User(tuple._1, tuple._2, tuple._3, tuple._4, tuple._5, tuple._6)

  def apply(token: GoogleToken): User =
    User(-1, token.name, token.email, None, Some(token.userId), 0)
}
