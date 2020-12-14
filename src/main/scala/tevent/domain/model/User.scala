package tevent.domain.model

case class User(id: Long,
                name: String,
                email: String,
                secretHash: String,
                lastRevoke: Long)
