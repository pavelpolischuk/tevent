package tevent.events.mock

import tevent.core.RepositoryError
import tevent.events.model.{EventParticipation, EventParticipationType}
import tevent.events.repository.EventParticipantsRepository
import tevent.user.model.User
import zio.test.mock
import zio.test.mock.{Expectation, Mock}
import zio.{Has, IO, URLayer, ZLayer}

object EventParticipantsRepositoryMock extends Mock[EventParticipantsRepository] {
  object GetParticipants extends Effect[Long, RepositoryError, List[(User, EventParticipationType)]]
  object Check extends Effect[(Long, Long), RepositoryError, Option[EventParticipationType]]
  object Add extends Effect[EventParticipation, RepositoryError, Unit]
  object Update extends Effect[EventParticipation, RepositoryError, Unit]
  object Remove extends Effect[(Long, Long), RepositoryError, Unit]

  private object empty extends Effect[Unit, Nothing, Unit]

  val Empty: Expectation[EventParticipantsRepository] = empty().optional

  override val compose: URLayer[Has[mock.Proxy], EventParticipantsRepository] = ZLayer.fromServiceM { proxy =>
    withRuntime.as {
      new EventParticipantsRepository.Service {
        override def getParticipants(eventId: Long): IO[RepositoryError, List[(User, EventParticipationType)]] = proxy(GetParticipants, eventId)
        override def check(userId: Long, eventId: Long): IO[RepositoryError, Option[EventParticipationType]] = proxy(Check, userId, eventId)
        override def add(participation: EventParticipation): IO[RepositoryError, Unit] = proxy(Add, participation)
        override def update(participation: EventParticipation): IO[RepositoryError, Unit] = proxy(Update, participation)
        override def remove(userId: Long, eventId: Long): IO[RepositoryError, Unit] = proxy(Remove, userId, eventId)
      }
    }
  }
}
