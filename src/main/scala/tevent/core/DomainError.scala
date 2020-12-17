package tevent.core

import zio.{IO, ZIO}

sealed trait DomainError {
  val message: String
}

case class EntityNotFound[A: EntityType, B](info: B)(implicit named: EntityType[A]) extends DomainError {
  override val message: String = s"${named.name} <$info> not found"
}

object EntityNotFound {
  def noneToNotFound[A: EntityType, B](info: B): Option[A] => IO[DomainError, A] = {
    case None => ZIO.fail(EntityNotFound[A, B](info))
    case Some(v) => ZIO.succeed(v)
  }
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