package tevent.user.model

import tevent.core.EntityType

sealed trait UserAccount

case class Email(value: String) extends UserAccount
case class GoogleId(value: String) extends UserAccount

object UserAccount {
  implicit object UserAccountEntity extends EntityType[UserAccount] {
    override val name: String = "Account"
  }
}
