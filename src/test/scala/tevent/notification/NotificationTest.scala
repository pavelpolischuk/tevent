package tevent.notification

import tevent.events.model.Event
import tevent.helpers.TestHelper.mappedAssert
import tevent.mock.{EmailMock, InMemoryOrganizationsRepository}
import tevent.organizations.dto.{OrgParticipationRequest => _}
import tevent.organizations.model.{OrgParticipationType, OrgSubscriber, Organization}
import tevent.user.model.User
import zio.Ref
import zio.test.Assertion.equalTo
import zio.test._
import zio.test.environment.TestEnvironment

import java.time.ZonedDateTime

object NotificationTest extends DefaultRunnableSpec {

  override def spec: ZSpec[TestEnvironment, Any] = suite("NotificationService")(

    testM("send emails to all receivers") {
      val sender = users.foldRight(EmailMock.Empty)((u, e) =>
        EmailMock.SendMail(mappedAssert[(String, String, String), String](_._1, equalTo(u.email))) ++ e)

      for {
        participants <- Ref.make(Map.empty[Long, List[(User, OrgParticipationType)]])
        org <- Ref.make(Map.empty[Long, Organization])
        orgRepo = InMemoryOrganizationsRepository.layer(org, participants)
        notifier = (orgRepo ++ sender) >>> Notification.live

        _ <- Notification.notifyNewEvent(organization, event, users).provideSomeLayer(notifier)
      } yield assertCompletes
    },

    testM("send emails to all subscribers of organization") {
      val sender = users.foldRight(EmailMock.Empty)((u, e) =>
        EmailMock.SendMail(mappedAssert[(String, String, String), String](_._1, equalTo(u.email))) ++ e)

      for {
        org <- Ref.make(Map(organization.id -> organization))
        participants <- Ref.make(Map[Long, List[(User, OrgParticipationType)]](organization.id -> users.map((_, OrgSubscriber))))

        orgRepo = InMemoryOrganizationsRepository.layer(org, participants)
        notifier = (orgRepo ++ sender) >>> Notification.live

        _ <- Notification.notifySubscribers(event).provideSomeLayer(notifier)
      } yield assertCompletes
    }

  )

  private val organization = Organization(1, "Paul Corp.", "pcorp", "Description", List("scala", "dev"))
  private val event = Event(1, organization.id, "Paul Meetup #1", "Meetup Description", ZonedDateTime.now(), Some("Moscow"), Some(1), Some("video"))
  private val users = List(User(1, "N1", "e1", "", 0), User(2, "N2", "e2", "", 0))
}
