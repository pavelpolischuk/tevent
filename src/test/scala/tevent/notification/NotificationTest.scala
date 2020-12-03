package tevent.notification

import tevent.domain.model.{Event, Organization, User}
import tevent.http.model.organization.{OrgParticipationRequest => _}
import tevent.mock.InMemoryEmailSender
import tevent.service.NotificationService
import zio.Ref
import zio.test.Assertion.hasSameElements
import zio.test._
import zio.test.environment.TestEnvironment

import java.time.ZonedDateTime

object NotificationTest extends DefaultRunnableSpec {

  override def spec: ZSpec[TestEnvironment, Any] = suite("NotificationService")(

    testM("send emails to all users") {
      for {
        receivers <- Ref.make(List.empty[String])
        sender = InMemoryEmailSender.layer(receivers)
        notifier = sender >>> NotificationService.live
        _ <- NotificationService.notifyNewEvent(organization, event, users).provideSomeLayer(notifier)
        receivers <- receivers.get
      } yield assert(receivers)(hasSameElements(users.map(_.email)))
    }

  )

  private val organization = Organization(1, "Paul Corp.")
  private val event = Event(1, organization.id, "Paul Meetup #1", ZonedDateTime.now(), Some("Moscow"), Some(1), Some("video"))
  private val users = List(User(1, "N1", "e1", ""), User(2, "N2", "e2", ""))
}
