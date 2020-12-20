package tevent.organizations

import io.circe.syntax.EncoderOps
import org.http4s.circe.CirceEntityCodec.{circeEntityDecoder, circeEntityEncoder}
import org.http4s.implicits.{http4sKleisliResponseSyntaxOptionT, http4sLiteralsSyntax}
import org.http4s.{Method, Request, Status}
import tevent.core.EntityNotFound
import tevent.helpers.AlwaysAuthMiddleware
import tevent.helpers.TestData._
import tevent.helpers.TestHelper.checkRequest
import tevent.organizations.mock.{OrganizationParticipantsMock, OrganizationsMock}
import tevent.organizations.model.{OrgParticipation, Organization}
import tevent.organizations.service.{OrganizationParticipants, Organizations}
import zio.RIO
import zio.interop.catz.taskConcurrentInstance
import zio.test.Assertion.equalTo
import zio.test._
import zio.test.environment.TestEnvironment
import zio.test.mock.Expectation

object OrganizationsEndpointTest extends DefaultRunnableSpec {
  type Task[A] = RIO[Organizations with OrganizationParticipants, A]

  override def spec: ZSpec[TestEnvironment, Any] = suite("OrganizationsEndpoint")(

    testM("should get NotFound for organization") {
      val organizations = OrganizationsMock.Get(equalTo(20L),
        Expectation.failure(EntityNotFound[Organization, Long](20)))
      val req = Request[Task](Method.GET, uri"/organizations/20")
      app.run(req).map(result => assert(result.status)(equalTo(Status.NotFound)))
        .provideSomeLayer(organizations ++ organizationParticipants)
    },
    testM("should post organization") {
      val organizations = OrganizationsMock.Create(equalTo((user.id, organization.copy(id = -1))), Expectation.value(organization))
      val postReq = Request[Task](Method.POST, uri"/organizations")
        .withEntity(organizationForm.asJson)
      checkRequest(app.run(postReq), Status.Ok, Some(organization))
        .provideSomeLayer(organizations ++ organizationParticipants)
    },
    testM("should get organization") {
      val organizations = OrganizationsMock.Get(equalTo(organization.id), Expectation.value(organization))
      val getReq = Request[Task](Method.GET, uri"/organizations/1")
      checkRequest(app.run(getReq), Status.Ok, Some(organization))
        .provideSomeLayer(organizations ++ organizationParticipants)
    },

    testM("should join") {
      val organizationParticipants = OrganizationParticipantsMock.JoinOrganization(
        equalTo(OrgParticipation(user.id, organization.id, memberRequestForm.participationType)), Expectation.unit)
      val postReq = Request[Task](Method.POST, uri"/organizations/1/join")
        .withEntity(memberRequestForm.asJson)
      app.run(postReq).map(res => assert(res.status)(equalTo(Status.Ok)))
        .provideSomeLayer(organizationParticipants ++ organizations)
    },
    testM("should get requests") {
      val organizationParticipants = OrganizationParticipantsMock.GetRequests(
        equalTo((user.id, organization.id)), Expectation.value(List((user2, memberRequest.participationType, user2))))
      val getReq = Request[Task](Method.GET, uri"/organizations/1/requests")
      checkRequest(app.run(getReq), Status.Ok, Some(List(memberRequest)))
        .provideSomeLayer(organizationParticipants ++ organizations)
    },
    testM("should approve participation") {
      val organizationParticipants = OrganizationParticipantsMock.ApproveRequest(
        equalTo((user2.id, organization.id, user.id)), Expectation.unit)
      val postApproveReq = Request[Task](Method.POST, uri"/organizations/1/approve")
        .withEntity(memberApprove.asJson)
      app.run(postApproveReq).map(res => assert(res.status)(equalTo(Status.Ok)))
        .provideSomeLayer(organizationParticipants ++ organizations)
    },
    testM("should get participants") {
      val organizationParticipants = OrganizationParticipantsMock.GetUsers(
        equalTo((user.id, organization.id)), Expectation.value(List((user2, memberRequest.participationType))))
      val getPartReq = Request[Task](Method.GET, uri"/organizations/1/users")
      checkRequest(app.run(getPartReq), Status.Ok, Some(List(memberParticipation)))
        .provideSomeLayer(organizationParticipants ++ organizations)
    },
    testM("should leave") {
      val organizationParticipants = OrganizationParticipantsMock.LeaveOrganization(
        equalTo((user.id, organization.id)), Expectation.unit)
      val postLeaveReq = Request[Task](Method.POST, uri"/organizations/1/leave")
      app.run(postLeaveReq).map(res => assert(res.status)(equalTo(Status.Ok)))
        .provideSomeLayer(organizationParticipants ++ organizations)
    }
  )

  private val endpoint = new OrganizationsEndpoint[Organizations with OrganizationParticipants]
  private val app = endpoint.routes(AlwaysAuthMiddleware(user)).orNotFound

  private def organizations = OrganizationsMock.Empty
  private def organizationParticipants = OrganizationParticipantsMock.Empty
}
