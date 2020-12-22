package tevent.core

sealed trait DomainError {
  val message: String
}

case class EntityNotFound[A: EntityType](info: A)(implicit named: EntityType[A]) extends DomainError {
  override val message: String = s"${named.name} <$info> not found"
}

case class ValidationError(override val message: String) extends DomainError

object TokenFormatInvalid extends ValidationError("Invalid token format")

object AccessError extends DomainError {
  override val message: String = "Forbidden"
}

case class ExecutionError(cause: Throwable) extends DomainError {
  override val message: String = cause.getMessage
}

case class RepositoryError(cause: Exception) extends DomainError {
  override val message: String = cause.getMessage
}
