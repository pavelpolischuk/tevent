package tevent.events.service

import tevent.core.EntityNotFound.noneToNotFound
import tevent.core.{DomainError, ValidationError}
import tevent.events.model._
import tevent.events.repository.EventsRepository
import tevent.notification.Notification
import tevent.organizations.model.OrgManager
import tevent.organizations.service.OrganizationParticipants
import zio.{IO, URLayer, ZIO, ZLayer}

object Events {
  trait Service {
    def get(id: Long): IO[DomainError, Event]
    def search(eventFilter: EventFilter): IO[DomainError, List[Event]]
    def getByUser(userId: Long): IO[DomainError, List[(Event, EventParticipationType)]]
    def update(userId: Long, event: Event): IO[DomainError, Unit]
    def create(userId: Long, event: Event): IO[DomainError, Event]
  }

  class EventsServiceImpl(events: EventsRepository.Service,
                          organizations: OrganizationParticipants.Service,
                          notification: Notification.Service) extends Events.Service {

    override def get(id: Long): IO[DomainError, Event] =
      events.getById(id).flatMap(noneToNotFound(id))

    override def update(userId: Long, event: Event): IO[DomainError, Unit] = for {
      old <- get(event.id)
      _ <- organizations.checkUser(userId, event.organizationId, OrgManager)
      _ <- IO.cond(old.organizationId != event.organizationId, (), ValidationError(s"Bad organization <${event.organizationId}>"))
      _ <- events.update(event)
    } yield ()

    override def create(userId: Long, event: Event): IO[DomainError, Event] = for {
      _ <- organizations.checkUser(userId, event.organizationId, OrgManager)
      eventId <- events.add(event)
      newEvent = event.copy(id = eventId)
      _ <- notification.notifySubscribers(newEvent)
    } yield newEvent

    override def search(eventFilter: EventFilter): IO[DomainError, List[Event]] =
      events.search(eventFilter)

    override def getByUser(userId: Long): IO[DomainError, List[(Event, EventParticipationType)]] =
      events.getByUser(userId)
  }


  def live: URLayer[EventsRepository with OrganizationParticipants with Notification, Events] =
    ZLayer.fromServices[EventsRepository.Service, OrganizationParticipants.Service, Notification.Service, Events.Service](
      new EventsServiceImpl(_, _, _))


  def get(id: Long): ZIO[Events, DomainError, Event] =
    ZIO.accessM(_.get.get(id))

  def search(eventFilter: EventFilter): ZIO[Events, DomainError, List[Event]] =
    ZIO.accessM(_.get.search(eventFilter))

  def getByUser(userId: Long): ZIO[Events, DomainError, List[(Event, EventParticipationType)]] =
    ZIO.accessM(_.get.getByUser(userId))

  def update(userId: Long, event: Event): ZIO[Events, DomainError, Unit] =
    ZIO.accessM(_.get.update(userId, event))

  def create(userId: Long, event: Event): ZIO[Events, DomainError, Event] =
    ZIO.accessM(_.get.create(userId, event))
}
