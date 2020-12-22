package tevent.events.mock

import tevent.core.RepositoryError
import tevent.events.dto.EventData
import tevent.events.model.{Event, EventFilter, EventParticipationType}
import tevent.events.repository.EventsRepository
import tevent.organizations.model.PlainOrganization
import zio.test.mock
import zio.test.mock.{Expectation, Mock}
import zio.{Has, IO, URLayer, ZLayer}

object EventsRepositoryMock extends Mock[EventsRepository] {
  object Add extends Effect[Event, RepositoryError, Long]
  object Search extends Effect[EventFilter, RepositoryError, List[(Event, PlainOrganization)]]
  object GetByUser extends Effect[Long, RepositoryError, List[(Event, EventParticipationType, PlainOrganization)]]
  object GetById extends Effect[Long, RepositoryError, Option[(Event, PlainOrganization)]]
  object Update extends Effect[Event, RepositoryError, Unit]

  private object empty extends Effect[Unit, Nothing, Unit]

  val Empty: Expectation[EventsRepository] = empty().optional

  override val compose: URLayer[Has[mock.Proxy], EventsRepository] = ZLayer.fromServiceM { proxy =>
    withRuntime.as {
      new EventsRepository.Service {
        override def add(event: Event): IO[RepositoryError, Long] = proxy(Add, event)
        override def search(eventFilter: EventFilter): IO[RepositoryError, List[(Event, PlainOrganization)]] = proxy(Search, eventFilter)
        override def getByUser(userId: Long): IO[RepositoryError, List[(Event, EventParticipationType, PlainOrganization)]] = proxy(GetByUser, userId)
        override def getById(id: Long): IO[RepositoryError, Option[(Event, PlainOrganization)]] = proxy(GetById, id)
        override def update(event: Event): IO[RepositoryError, Unit] = proxy(Update, event)
      }
    }
  }
}
