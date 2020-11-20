package tevent.infrastructure.repository.tables

import slick.jdbc.JdbcProfile
import slick.lifted.ProvenShape
import tevent.domain.model.User


class UsersTable(val profile: JdbcProfile)  {
  import profile.api._

  class Users(tag: Tag) extends Table[User](tag, "USERS") {
    def id: Rep[Long] = column("ID", O.PrimaryKey, O.AutoInc)
    def name: Rep[String] = column("NAME")
    def email: Rep[String] = column("EMAIL", O.Unique)
    def secretHash: Rep[String] = column("SECRET")

    override def * : ProvenShape[User] = (id, name, email, secretHash).mapTo[User]
  }

  val All: TableQuery[Users] = TableQuery[Users]

  def all: DBIO[Seq[User]] = All.result

  def add(user: User): DBIO[Long] =
    (All.map(u => (u.name, u.email, u.secretHash)) returning All.map(_.id)) +=
      (user.name, user.email, user.secretHash)

  def withId(id: Long): DBIO[Option[User]] = All.filter(_.id === id).result.headOption

  def withEmail(email: String): DBIO[Option[User]] = All.filter(_.email === email).result.headOption

  def update(user: User): DBIO[Int] = {
    val q = for { c <- All if c.id === user.id } yield (c.email, c.name)
    q.update((user.email, user.name))
  }
}
