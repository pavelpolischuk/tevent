package tevent.notification

import tevent.helpers.TestData._
import tevent.helpers.TestHelper.mappedAssert
import tevent.organizations.mock.{OrganizationParticipantsRepositoryMock, OrganizationsMock}
import tevent.notification.mock.EmailMock
import tevent.organizations.dto.{OrgParticipationRequest => _}
import tevent.organizations.model.{OrgManager, OrgSubscriber}
import zio.test.Assertion.equalTo
import zio.test._
import zio.test.environment.TestEnvironment
import zio.test.mock.Expectation

object NotificationTest extends DefaultRunnableSpec {

  override def spec: ZSpec[TestEnvironment, Any] = suite("Notification")(

    testM("send emails to all receivers") {
      val repo = OrganizationsMock.Empty ++ OrganizationParticipantsRepositoryMock.Empty
      val notifier = sender ++ repo >>> Notification.live
      for {
        _ <- Notification.notifyNewEvent(organization, event, List(user, user2)).provideSomeLayer(notifier)
      } yield assertCompletes
    },

    testM("send emails to all subscribers of organization") {
      val organizations = OrganizationsMock.Get(equalTo(organization.typedId), Expectation.value(organization))
      val participants = OrganizationParticipantsRepositoryMock.GetParticipants(
        equalTo(organization.id), Expectation.value(List((user, OrgManager), (user2, OrgSubscriber))))
      val notifier = organizations ++ participants ++ sender >>> Notification.live
      for {
        _ <- Notification.notifySubscribers(event).provideSomeLayer(notifier)
      } yield assertCompletes
    }

  )

  private def sender =
    EmailMock.SendMail(mappedAssert[(String, String, String), String](_._1, equalTo(user.email))) and
    EmailMock.SendMail(mappedAssert[(String, String, String), String](_._1, equalTo(user2.email)))
}
