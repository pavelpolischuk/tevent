package tevent.notification

import tevent.events.model.Event
import tevent.helpers.TestHelper.mappedAssert
import tevent.mock.{EmailMock, OrganizationParticipantsRepositoryMock, OrganizationsMock}
import tevent.organizations.dto.{OrgParticipationRequest => _}
import tevent.organizations.model.{OrgSubscriber, Organization}
import tevent.user.model.User
import zio.test.Assertion.equalTo
import zio.test._
import zio.test.environment.TestEnvironment
import zio.test.mock.Expectation

import java.time.ZonedDateTime

object NotificationTest extends DefaultRunnableSpec {

  override def spec: ZSpec[TestEnvironment, Any] = suite("Notification")(

    testM("send emails to all receivers") {
      val repo = OrganizationsMock.Empty ++ OrganizationParticipantsRepositoryMock.Empty
      val notifier = sender ++ repo >>> Notification.live
      for {
        _ <- Notification.notifyNewEvent(organization, event, users).provideSomeLayer(notifier)
      } yield assertCompletes
    },

    testM("send emails to all subscribers of organization") {
      val organizations = OrganizationsMock.Get(equalTo(organization.id), Expectation.value(organization))
      val participants = OrganizationParticipantsRepositoryMock.GetParticipants(equalTo(organization.id), Expectation.value(users.map((_, OrgSubscriber))))
      val notifier = organizations ++ participants ++ sender >>> Notification.live
      for {
        _ <- Notification.notifySubscribers(event).provideSomeLayer(notifier)
      } yield assertCompletes
    }

  )

  private val organization = Organization(1, "Paul Corp.", "pcorp", "Description", List("scala", "dev"))
  private val event = Event(1, organization.id, "Paul Meetup #1", "Meetup Description", ZonedDateTime.now(), Some("Moscow"), Some(1), Some("video"))
  private val users = List(User(1, "N1", "e1", "", 0), User(2, "N2", "e2", "", 0))

  private def sender = users.foldRight(EmailMock.Empty)((u, e) =>
    EmailMock.SendMail(mappedAssert[(String, String, String), String](_._1, equalTo(u.email))) ++ e)
}
