package tevent.service

import tevent.domain.EntityNotFound.optionToIO
import tevent.domain.Named.eventNamed
import tevent.domain.model.{Event, EventParticipationType, OrgManager}
import tevent.domain.repository.EventsRepository
import tevent.domain.{DomainError, ValidationError}
import zio.{IO, URLayer, ZIO, ZLayer}

import java.time.ZonedDateTime

object EventsService {
  trait Service {
    def get(id: Long): IO[DomainError, Option[Event]]
    def getByOrganization(organizationId: Long): IO[DomainError, List[Event]]
    def getByUser(userId: Long): IO[DomainError, List[(Event, EventParticipationType)]]
    def update(userId: Long, event: Event): IO[DomainError, Unit]
    def create(userId: Long, event: Event): IO[DomainError, Event]
  }

  class EventsServiceImpl(events: EventsRepository.Service, participation: ParticipationService.Service) extends EventsService.Service {

    override def get(id: Long): IO[DomainError, Option[Event]] = events.getById(id)

    override def update(userId: Long, event: Event): IO[DomainError, Unit] = for {
      old <- get(event.id).flatMap(optionToIO[Event, Long](event.id))
      _ <- participation.checkUser(userId, event.organizationId, OrgManager)
      _ <- IO.cond(old.organizationId != event.organizationId, (), ValidationError(s"Bad organization <${event.organizationId}>"))
      _ <- events.update(event)
    } yield ()

    override def create(userId: Long, event: Event): IO[DomainError, Event] = for {
      _ <- participation.checkUser(userId, event.organizationId, OrgManager)
      eventId <- events.add(event)
    } yield event.copy(id = eventId)

    override def getByOrganization(organizationId: Long): IO[DomainError, List[Event]] =
      events.getByOrganization(organizationId)

    override def getByUser(userId: Long): IO[DomainError, List[(Event, EventParticipationType)]] =
      events.getByUser(userId)
  }

  def live: URLayer[EventsRepository with ParticipationService, EventsService] =
    ZLayer.fromServices[EventsRepository.Service, ParticipationService.Service, EventsService.Service](new EventsServiceImpl(_, _))

  def get(id: Long): ZIO[EventsService, DomainError, Option[Event]] =
    ZIO.accessM(_.get.get(id))

  def getByOrganization(organizationId: Long): ZIO[EventsService, DomainError, List[Event]] =
    ZIO.accessM(_.get.getByOrganization(organizationId))

  def getByUser(userId: Long): ZIO[EventsService, DomainError, List[(Event, EventParticipationType)]] =
    ZIO.accessM(_.get.getByUser(userId))

  def update(userId: Long, event: Event): ZIO[EventsService, DomainError, Unit] =
    ZIO.accessM(_.get.update(userId, event))

  def create(userId: Long, organizationId: Long, name: String, datetime: ZonedDateTime, location: Option[String],
             capacity: Option[Int], videoBroadcastLink: Option[String]): ZIO[EventsService, DomainError, Event] =
    ZIO.accessM(_.get.create(userId, Event(-1, organizationId, name, datetime, location, capacity, videoBroadcastLink)))
}
