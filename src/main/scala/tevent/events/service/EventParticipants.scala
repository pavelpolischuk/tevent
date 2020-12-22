package tevent.events.service

import tevent.core.{DomainError, EntityNotFound, ValidationError}
import tevent.events.model._
import tevent.events.repository.{EventParticipantsRepository, EventsRepository}
import tevent.organizations.model.{OrgMember, OrganizationId}
import tevent.organizations.service.OrganizationParticipants
import tevent.user.model.{User, UserId}
import zio.{IO, URLayer, ZIO, ZLayer}

object EventParticipants {
  trait Service {
    def getUsers(userId: UserId, eventId: EventId): IO[DomainError, List[(User, EventParticipationType)]]
    def joinEvent(participation: EventParticipation): IO[DomainError, Unit]
    def leaveEvent(userId: UserId, eventId: EventId): IO[DomainError, Unit]
  }

  class EventParticipantsImpl(events: EventsRepository.Service,
                              participants: EventParticipantsRepository.Service,
                              organizations: OrganizationParticipants.Service) extends EventParticipants.Service {

    override def getUsers(userId: UserId, eventId: EventId): IO[DomainError, List[(User, EventParticipationType)]] = for {
      (event, _) <- events.getById(eventId.id).someOrFail(EntityNotFound(eventId))
      _ <- organizations.checkUser(userId, OrganizationId(event.organizationId), OrgMember)
      users <- participants.getParticipants(eventId.id)
    } yield users

    // ToDo: rewrite with count field and good for concurrency
    private def checkEmptyPlaces(eventId: Long, capacity: Int): IO[DomainError, Unit] =
      participants.getParticipants(eventId)
        .filterOrFail[DomainError](_.count(_._2 == OfflineParticipant) < capacity)(
          ValidationError(s"Empty offline seats are out for event <$eventId>")).unit

    override def joinEvent(participation: EventParticipation): IO[DomainError, Unit] = for {
      (event, _) <- events.getById(participation.eventId).someOrFail(EntityNotFound(EventId(participation.eventId)))
      oldType <- participants.check(participation.userId, participation.eventId)
      _ <- (event.capacity, participation.participationType, oldType) match {
        case (Some(v), OfflineParticipant, Some(OnlineParticipant) | Some(EventSubscriber) | None) => checkEmptyPlaces(event.id, v)
        case _ => IO.unit
      }
      _ <- if (oldType.isEmpty) participants.add(participation) else participants.update(participation)
    } yield ()

    override def leaveEvent(userId: UserId, eventId: EventId): IO[DomainError, Unit] =
      participants.remove(userId.id, eventId.id)
  }


  def live: URLayer[EventsRepository with EventParticipantsRepository with OrganizationParticipants, EventParticipants] =
    ZLayer.fromServices[EventsRepository.Service, EventParticipantsRepository.Service, OrganizationParticipants.Service, EventParticipants.Service](
      new EventParticipantsImpl(_, _, _))


  def getUsers(userId: UserId, eventId: EventId): ZIO[EventParticipants, DomainError, List[(User, EventParticipationType)]] =
    ZIO.accessM(_.get.getUsers(userId, eventId))

  def joinEvent(participation: EventParticipation): ZIO[EventParticipants, DomainError, Unit] =
    ZIO.accessM(_.get.joinEvent(participation))

  def leaveEvent(userId: UserId, eventId: EventId): ZIO[EventParticipants, DomainError, Unit] =
    ZIO.accessM(_.get.leaveEvent(userId, eventId))
}
