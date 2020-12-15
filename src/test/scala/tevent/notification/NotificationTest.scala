package tevent.notification

import tevent.organizations.model.{OrgParticipationType, OrgSubscriber, Organization}
import tevent.events.model.Event
import tevent.organizations.dto.{OrgParticipationRequest => _}
import tevent.mock.{InMemoryEmailSender, InMemoryOrganizationsRepository}
import tevent.user.model.User
import zio.Ref
import zio.test.Assertion.hasSameElements
import zio.test._
import zio.test.environment.TestEnvironment

import java.time.ZonedDateTime

object NotificationTest extends DefaultRunnableSpec {

  override def spec: ZSpec[TestEnvironment, Any] = suite("NotificationService")(

    testM("send emails to all receivers") {
      for {
        receivers <- Ref.make(List.empty[String])
        org <- Ref.make(Map.empty[Long, Organization])
        participants <- Ref.make(Map.empty[Long, List[(User, OrgParticipationType)]])

        sender = InMemoryEmailSender.layer(receivers)
        orgRepo = InMemoryOrganizationsRepository.layer(org, participants)
        notifier = (orgRepo ++ sender) >>> Notification.live

        _ <- Notification.notifyNewEvent(organization, event, users).provideSomeLayer(notifier)
        receivers <- receivers.get
      } yield assert(receivers)(hasSameElements(users.map(_.email)))
    },

    testM("send emails to all subscribers of organization") {
      for {
        receivers <- Ref.make(List.empty[String])
        org <- Ref.make(Map(organization.id -> organization))
        participants <- Ref.make(Map[Long, List[(User, OrgParticipationType)]](organization.id -> users.map((_, OrgSubscriber))))

        sender = InMemoryEmailSender.layer(receivers)
        orgRepo = InMemoryOrganizationsRepository.layer(org, participants)
        notifier = (orgRepo ++ sender) >>> Notification.live

        _ <- Notification.notifySubscribers(event).provideSomeLayer(notifier)
        receivers <- receivers.get
      } yield assert(receivers)(hasSameElements(users.map(_.email)))
    }

  )

  private val organization = Organization(1, "Paul Corp.", "pcorp", "Description", List("scala", "dev"))
  private val event = Event(1, organization.id, "Paul Meetup #1", "Meetup Description", ZonedDateTime.now(), Some("Moscow"), Some(1), Some("video"))
  private val users = List(User(1, "N1", "e1", "", 0), User(2, "N2", "e2", "", 0))
}
