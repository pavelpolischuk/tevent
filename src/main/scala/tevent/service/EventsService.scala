package tevent.service

import tevent.domain.EntityNotFound.noneToNotFound
import tevent.domain.Named.eventNamed
import tevent.domain.model.{Event, EventFilter, EventParticipation, EventParticipationType, EventSubscriber, OfflineParticipant, OnlineParticipant, OrgManager, OrgMember, User}
import tevent.domain.repository.EventsRepository
import tevent.domain.{DomainError, ValidationError}
import zio.{IO, URLayer, ZIO, ZLayer}

import java.time.ZonedDateTime

object EventsService {
  trait Service {
    def get(id: Long): IO[DomainError, Option[Event]]
    def getUsers(userId: Long, eventId: Long): IO[DomainError, List[(User, EventParticipationType)]]
    def search(eventFilter: EventFilter): IO[DomainError, List[Event]]
    def getByUser(userId: Long): IO[DomainError, List[(Event, EventParticipationType)]]
    def update(userId: Long, event: Event): IO[DomainError, Unit]
    def create(userId: Long, event: Event): IO[DomainError, Event]

    def joinEvent(participation: EventParticipation): IO[DomainError, Unit]
    def leaveEvent(userId: Long, eventId: Long): IO[DomainError, Unit]
  }

  class EventsServiceImpl(events: EventsRepository.Service,
                          participation: ParticipationService.Service,
                          notification: NotificationService.Service) extends EventsService.Service {

    override def get(id: Long): IO[DomainError, Option[Event]] = events.getById(id)

    override def getUsers(userId: Long, eventId: Long): IO[DomainError, List[(User, EventParticipationType)]] = for {
      event <- get(eventId).flatMap(noneToNotFound(eventId))
      _ <- participation.checkUser(userId, event.organizationId, OrgMember)
      users <- events.getUsers(eventId)
    } yield users

    override def update(userId: Long, event: Event): IO[DomainError, Unit] = for {
      old <- get(event.id).flatMap(noneToNotFound(event.id))
      _ <- participation.checkUser(userId, event.organizationId, OrgManager)
      _ <- IO.cond(old.organizationId != event.organizationId, (), ValidationError(s"Bad organization <${event.organizationId}>"))
      _ <- events.update(event)
    } yield ()

    override def create(userId: Long, event: Event): IO[DomainError, Event] = for {
      _ <- participation.checkUser(userId, event.organizationId, OrgManager)
      eventId <- events.add(event)
      newEvent = event.copy(id = eventId)
      _ <- notification.notifySubscribers(newEvent)
    } yield newEvent

    override def search(eventFilter: EventFilter): IO[DomainError, List[Event]] =
      events.search(eventFilter)

    override def getByUser(userId: Long): IO[DomainError, List[(Event, EventParticipationType)]] =
      events.getByUser(userId)

    private def checkEmptyPlaces(eventId: Long, capacity: Int): IO[DomainError, Unit] =
      events.getUsers(eventId)
        .filterOrFail[DomainError](_.count(_._2 == OfflineParticipant) < capacity)(
          ValidationError(s"Empty offline seats are out for event <$eventId>")).unit

    override def joinEvent(participation: EventParticipation): IO[DomainError, Unit] = for {
      event <- events.getById(participation.eventId).flatMap(noneToNotFound(participation.eventId))
      oldType <- events.checkUser(participation.userId, participation.eventId)
      _ <- (event.capacity, participation.participationType, oldType) match {
        case (Some(v), OfflineParticipant, Some(OnlineParticipant) | Some(EventSubscriber) | None) => checkEmptyPlaces(event.id, v)
        case _ => IO.unit
      }
      _ <- if (oldType.isEmpty) events.addUser(participation) else events.updateUser(participation)
    } yield ()

    override def leaveEvent(userId: Long, eventId: Long): IO[DomainError, Unit] =
      events.removeUser(userId, eventId)
  }

  def live: URLayer[EventsRepository with ParticipationService with NotificationService, EventsService] =
    ZLayer.fromServices[EventsRepository.Service, ParticipationService.Service, NotificationService.Service, EventsService.Service](
      new EventsServiceImpl(_, _, _))

  def get(id: Long): ZIO[EventsService, DomainError, Option[Event]] =
    ZIO.accessM(_.get.get(id))

  def getUsers(userId: Long, eventId: Long): ZIO[EventsService, DomainError, List[(User, EventParticipationType)]] =
    ZIO.accessM(_.get.getUsers(userId, eventId))

  def search(eventFilter: EventFilter): ZIO[EventsService, DomainError, List[Event]] =
    ZIO.accessM(_.get.search(eventFilter))

  def getByUser(userId: Long): ZIO[EventsService, DomainError, List[(Event, EventParticipationType)]] =
    ZIO.accessM(_.get.getByUser(userId))

  def update(userId: Long, event: Event): ZIO[EventsService, DomainError, Unit] =
    ZIO.accessM(_.get.update(userId, event))

  def create(userId: Long, event: Event): ZIO[EventsService, DomainError, Event] =
    ZIO.accessM(_.get.create(userId, event))

  def joinEvent(participation: EventParticipation): ZIO[EventsService, DomainError, Unit] =
    ZIO.accessM(_.get.joinEvent(participation))

  def leaveEvent(userId: Long, eventId: Long): ZIO[EventsService, DomainError, Unit] =
    ZIO.accessM(_.get.leaveEvent(userId, eventId))
}
