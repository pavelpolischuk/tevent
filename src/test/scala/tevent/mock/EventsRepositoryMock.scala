package tevent.mock

import tevent.domain.RepositoryError
import tevent.domain.model._
import tevent.domain.repository.EventsRepository
import zio.test.mock
import zio.{Has, IO, URLayer, ZLayer}
import zio.test.mock.{Expectation, Mock}

object EventsRepositoryMock extends Mock[EventsRepository] {
  object Add extends Effect[Event, RepositoryError, Long]
  object Search extends Effect[EventFilter, RepositoryError, List[Event]]
  object GetByUser extends Effect[Long, RepositoryError, List[(Event, EventParticipationType)]]
  object GetById extends Effect[Long, RepositoryError, Option[Event]]
  object Update extends Effect[Event, RepositoryError, Unit]

  object GetUsers extends Effect[Long, RepositoryError, List[(User, EventParticipationType)]]
  object CheckUser extends Effect[(Long, Long), RepositoryError, Option[EventParticipationType]]
  object AddUser extends Effect[EventParticipation, RepositoryError, Unit]
  object UpdateUser extends Effect[EventParticipation, RepositoryError, Unit]
  object RemoveUser extends Effect[(Long, Long), RepositoryError, Unit]

  private object empty extends Effect[Unit, Nothing, Unit]

  val Empty: Expectation[EventsRepository] = empty().optional

  override val compose: URLayer[Has[mock.Proxy], EventsRepository] = ZLayer.fromServiceM { proxy =>
    withRuntime.as {
      new EventsRepository.Service {
        override def add(event: Event): IO[RepositoryError, Long] = proxy(Add, event)
        override def search(eventFilter: EventFilter): IO[RepositoryError, List[Event]] = proxy(Search, eventFilter)
        override def getByUser(userId: Long): IO[RepositoryError, List[(Event, EventParticipationType)]] = proxy(GetByUser, userId)
        override def getById(id: Long): IO[RepositoryError, Option[Event]] = proxy(GetById, id)
        override def update(event: Event): IO[RepositoryError, Unit] = proxy(Update, event)
        override def getUsers(eventId: Long): IO[RepositoryError, List[(User, EventParticipationType)]] = proxy(GetUsers, eventId)
        override def checkUser(userId: Long, eventId: Long): IO[RepositoryError, Option[EventParticipationType]] = proxy(CheckUser, userId, eventId)
        override def addUser(participation: EventParticipation): IO[RepositoryError, Unit] = proxy(AddUser, participation)
        override def updateUser(participation: EventParticipation): IO[RepositoryError, Unit] = proxy(UpdateUser, participation)
        override def removeUser(userId: Long, eventId: Long): IO[RepositoryError, Unit] = proxy(RemoveUser, userId, eventId)
      }
    }
  }
}
