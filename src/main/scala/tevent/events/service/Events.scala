package tevent.events.service

import tevent.core.{DomainError, EntityNotFound, ValidationError}
import tevent.events.dto.EventData
import tevent.events.model._
import tevent.events.repository.EventsRepository
import tevent.notification.Notification
import tevent.organizations.model.{OrgManager, OrganizationId}
import tevent.organizations.service.OrganizationParticipants
import tevent.user.model.UserId
import zio.{IO, URLayer, ZIO, ZLayer}

object Events {
  trait Service {
    def get(id: EventId): IO[DomainError, EventData]
    def search(eventFilter: EventFilter): IO[DomainError, List[EventData]]
    def getByUser(userId: UserId): IO[DomainError, List[(EventData, EventParticipationType)]]
    def update(userId: UserId, event: Event): IO[DomainError, Unit]
    def create(userId: UserId, event: Event): IO[DomainError, Event]
  }

  class EventsServiceImpl(events: EventsRepository.Service,
                          organizations: OrganizationParticipants.Service,
                          notification: Notification.Service) extends Events.Service {

    override def get(id: EventId): IO[DomainError, EventData] =
      events.getById(id.id).someOrFail(EntityNotFound(id)).map(EventData.apply)

    override def update(userId: UserId, event: Event): IO[DomainError, Unit] = for {
      old <- get(EventId(event.id))
      _ <- organizations.checkUser(userId, OrganizationId(event.organizationId), OrgManager)
      _ <- IO.cond(old.organization.id != event.organizationId, (), ValidationError(s"Bad organization <${event.organizationId}>"))
      _ <- events.update(event)
    } yield ()

    override def create(userId: UserId, event: Event): IO[DomainError, Event] = for {
      _ <- organizations.checkUser(userId, OrganizationId(event.organizationId), OrgManager)
      eventId <- events.add(event)
      newEvent = event.copy(id = eventId)
      _ <- notification.notifySubscribers(newEvent)
    } yield newEvent

    override def search(eventFilter: EventFilter): IO[DomainError, List[EventData]] =
      events.search(eventFilter).map(_.map(EventData.apply))

    override def getByUser(userId: UserId): IO[DomainError, List[(EventData, EventParticipationType)]] =
      events.getByUser(userId.id).map(_.map(e => (EventData(e._1, e._3), e._2)))
  }


  def live: URLayer[EventsRepository with OrganizationParticipants with Notification, Events] =
    ZLayer.fromServices[EventsRepository.Service, OrganizationParticipants.Service, Notification.Service, Events.Service](
      new EventsServiceImpl(_, _, _))


  def get(id: EventId): ZIO[Events, DomainError, EventData] =
    ZIO.accessM(_.get.get(id))

  def search(eventFilter: EventFilter): ZIO[Events, DomainError, List[EventData]] =
    ZIO.accessM(_.get.search(eventFilter))

  def getByUser(userId: UserId): ZIO[Events, DomainError, List[(EventData, EventParticipationType)]] =
    ZIO.accessM(_.get.getByUser(userId))

  def update(userId: UserId, event: Event): ZIO[Events, DomainError, Unit] =
    ZIO.accessM(_.get.update(userId, event))

  def create(userId: UserId, event: Event): ZIO[Events, DomainError, Event] =
    ZIO.accessM(_.get.create(userId, event))
}
