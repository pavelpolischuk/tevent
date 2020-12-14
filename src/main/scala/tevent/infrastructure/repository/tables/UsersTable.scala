package tevent.infrastructure.repository.tables

import slick.jdbc.JdbcProfile
import slick.lifted.ProvenShape
import tevent.domain.model.User


class UsersTable(implicit val profile: JdbcProfile)  {
  import profile.api._

  class Users(tag: Tag) extends Table[User](tag, "USERS") {
    def id: Rep[Long] = column("ID", O.PrimaryKey, O.AutoInc)
    def name: Rep[String] = column("NAME")
    def email: Rep[String] = column("EMAIL", O.Unique)
    def secretHash: Rep[String] = column("SECRET")
    def lastRevoke: Rep[Long] = column("LAST_REVOKE")

    override def * : ProvenShape[User] = (id, name, email, secretHash, lastRevoke).mapTo[User]
  }

  val All: TableQuery[Users] = TableQuery[Users]

  def all: DBIO[Seq[User]] = All.result

  def add(user: User): DBIO[Long] =
    (All.map(u => (u.name, u.email, u.secretHash, u.lastRevoke)) returning All.map(_.id)) +=
      (user.name, user.email, user.secretHash, user.lastRevoke)

  def withId(id: Long): DBIO[Option[User]] = All.filter(_.id === id).result.headOption

  def withEmail(email: String): DBIO[Option[User]] = All.filter(_.email === email).result.headOption

  def updateInfo(id: Long, name: String, email: String): DBIO[Int] = {
    val q = for { c <- All if c.id === id } yield (c.email, c.name)
    q.update((email, name))
  }

  def changeSecret(id: Long, secret: String, lastRevoke: Long): DBIO[Int] = {
    val q = for { c <- All if c.id === id } yield (c.secretHash, c.lastRevoke)
    q.update((secret, lastRevoke))
  }

  def revokeAccess(id: Long, lastRevoke: Long): DBIO[Int] = {
    val q = for { c <- All if c.id === id } yield c.lastRevoke
    q.update(lastRevoke)
  }
}
