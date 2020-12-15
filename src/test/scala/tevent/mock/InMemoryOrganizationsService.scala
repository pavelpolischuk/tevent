package tevent.mock

import tevent.core.{DomainError, EntityNotFound}
import tevent.events.model.Event.EventEntity
import tevent.organizations.model.{Organization, OrganizationFilter}
import tevent.organizations.service.Organizations
import zio._

class InMemoryOrganizationsService(ref: Ref[Map[Long, Organization]], counter: Ref[Long])
  extends Organizations.Service {

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

  override def create(userId: Long, organization: Organization): IO[DomainError, Organization] = for {
    newId <- counter.updateAndGet(_ + 1)
    org   = organization.copy(id = newId)
    _     <- ref.update(store => store + (newId -> org))
  } yield org

  override def search(filter: OrganizationFilter): IO[DomainError, List[Organization]] =
    if (filter.isEmpty) ref.get.map(_.values.toList)
    else ref.get.map(_.values.filter(org => org.tags.exists(filter.tags.contains(_))).toList)
}

object InMemoryOrganizationsService {
  def layer: ULayer[Organizations] = (for {
    ref <- Ref.make(Map.empty[Long, Organization])
    counter <- Ref.make(0L)
  } yield new InMemoryOrganizationsService(ref, counter)).toLayer
}
