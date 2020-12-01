package tevent.mock

import tevent.domain.Named.organizationNamed
import tevent.domain.{DomainError, EntityNotFound}
import tevent.domain.model.Organization
import tevent.service.OrganizationsService
import zio._

class InMemoryOrganizationsService(ref: Ref[Map[Long, Organization]], counter: Ref[Long])
  extends OrganizationsService.Service {

  override def get(id: Long): IO[DomainError, Option[Organization]] =
    ref.get.map(_.get(id))

  override def update(userId: Long, organization: Organization): IO[DomainError, Unit] =
    for {
      oldValue <- get(organization.id)
      result   <- oldValue.fold[IO[DomainError, Unit]](IO.fail(EntityNotFound(organization.id))) { x =>
        val newValue = x.copy(name = organization.name)
        ref.update(store => store + (newValue.id -> newValue))
      }
    } yield result

  override def create(userId: Long, name: String): IO[DomainError, Organization] = for {
    newId <- counter.updateAndGet(_ + 1)
    org   = Organization(newId, name)
    _     <- ref.update(store => store + (newId -> org))
  } yield org
}

object InMemoryOrganizationsService {
  def layer: ULayer[OrganizationsService] = (for {
    ref <- Ref.make(Map.empty[Long, Organization])
    counter <- Ref.make(0L)
  } yield new InMemoryOrganizationsService(ref, counter)).toLayer
}
