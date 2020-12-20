package tevent.user.model

sealed trait UserAccount

case class Email(value: String) extends UserAccount
case class GoogleId(value: String) extends UserAccount
