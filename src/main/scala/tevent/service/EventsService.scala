package tevent.service

import tevent.domain.model.Event
import tevent.domain.repository.EventsRepository
import tevent.domain.DomainError
import zio.{IO, URLayer, ZLayer}

object EventsService {
  trait Service {
    def get(id: Long): IO[DomainError, Option[Event]]
  }

  class EventsServiceImpl(repository: EventsRepository.Service) extends EventsService.Service {

    override def get(id: Long): IO[DomainError, Option[Event]] = repository.getById(id)
  }

  def live: URLayer[EventsRepository, EventsService] = ZLayer.fromService(new EventsServiceImpl(_))
}
