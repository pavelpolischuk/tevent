package tevent.service

import tevent.domain.DomainError
import tevent.domain.model.Organization
import tevent.domain.repository.OrganizationsRepository
import zio.{IO, URLayer, ZLayer}

object OrganizationsService {
  trait Service {
    def get(id: Long): IO[DomainError, Option[Organization]]
  }

  class OrganizationsServiceImpl(repository: OrganizationsRepository.Service) extends OrganizationsService.Service {

    override def get(id: Long): IO[DomainError, Option[Organization]] = repository.getById(id)
  }

  def live: URLayer[OrganizationsRepository, OrganizationsService] = ZLayer.fromService(new OrganizationsServiceImpl(_))
}
