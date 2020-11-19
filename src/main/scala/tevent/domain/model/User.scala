package tevent.domain.model

case class User(id: Option[Long],
                name: String,
                email: String,
                secretHash: String)
