package tevent.mock

import tevent.core.RepositoryError
import tevent.events.model.{Event, EventFilter, EventParticipation, EventParticipationType}
import tevent.events.repository.EventsRepository
import tevent.user.model.User
import zio.test.mock
import zio.{Has, IO, URLayer, ZLayer}
import zio.test.mock.{Expectation, Mock}

object EventsRepositoryMock extends Mock[EventsRepository] {
  object Add extends Effect[Event, RepositoryError, Long]
  object Search extends Effect[EventFilter, RepositoryError, List[Event]]
  object GetByUser extends Effect[Long, RepositoryError, List[(Event, EventParticipationType)]]
  object GetById extends Effect[Long, RepositoryError, Option[Event]]
  object Update extends Effect[Event, RepositoryError, Unit]

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
      }
    }
  }
}
