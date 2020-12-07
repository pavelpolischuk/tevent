package tevent.mock

import tevent.domain.DomainError
import tevent.domain.model.{Event, Organization, User}
import tevent.service.NotificationService
import zio.test.mock
import zio.test.mock.{Expectation, Mock}
import zio.{Has, IO, URLayer, ZLayer}

object NotificationServiceMock extends Mock[NotificationService] {
  object NotifySubscribers extends Effect[Event, DomainError, Unit]
  object NotifyNewEvent extends Effect[(Organization, Event, List[User]), DomainError, Unit]
  private object empty extends Effect[Unit, Nothing, Unit]

  val Empty: Expectation[NotificationService] = empty().optional

  override val compose: URLayer[Has[mock.Proxy], NotificationService] = ZLayer.fromServiceM { proxy =>
    withRuntime.as {
      new NotificationService.Service {
        override def notifySubscribers(event: Event): IO[DomainError, Unit] = proxy(NotifySubscribers, event)
        override def notifyNewEvent(organization: Organization, event: Event, users: List[User]): IO[DomainError, Unit] =
          proxy(NotifyNewEvent, organization, event, users)
      }
    }
  }
}
