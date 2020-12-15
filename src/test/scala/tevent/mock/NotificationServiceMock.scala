package tevent.mock

import tevent.core.DomainError
import tevent.organizations.model.Organization
import tevent.events.model.Event
import tevent.notification.Notification
import tevent.user.model.User
import zio.test.mock
import zio.test.mock.{Expectation, Mock}
import zio.{Has, IO, URLayer, ZLayer}

object NotificationServiceMock extends Mock[Notification] {
  object NotifySubscribers extends Effect[Event, DomainError, Unit]
  object NotifyNewEvent extends Effect[(Organization, Event, List[User]), DomainError, Unit]
  private object empty extends Effect[Unit, Nothing, Unit]

  val Empty: Expectation[Notification] = empty().optional

  override val compose: URLayer[Has[mock.Proxy], Notification] = ZLayer.fromServiceM { proxy =>
    withRuntime.as {
      new Notification.Service {
        override def notifySubscribers(event: Event): IO[DomainError, Unit] = proxy(NotifySubscribers, event)
        override def notifyNewEvent(organization: Organization, event: Event, users: List[User]): IO[DomainError, Unit] =
          proxy(NotifyNewEvent, organization, event, users)
      }
    }
  }
}
