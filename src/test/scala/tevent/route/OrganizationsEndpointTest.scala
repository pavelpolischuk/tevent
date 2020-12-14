package tevent.route

import org.http4s.implicits.{http4sKleisliResponseSyntaxOptionT, http4sLiteralsSyntax}
import org.http4s.{Method, Request, Status}
import io.circe.syntax.EncoderOps
import org.http4s.circe.jsonEncoder
import tevent.domain.model._
import tevent.http.endpoints.{OrganizationsEndpoint, circeJsonDecoder}
import tevent.http.model.organization.{OrgParticipationApprove, OrgParticipationForm, OrgParticipationRequest, OrgUserParticipationData, OrganizationForm}
import tevent.http.model.user.UserId
import tevent.mock.{InMemoryOrganizationsService, InMemoryParticipationService}
import tevent.service.{OrganizationsService, ParticipationService}
import zio.interop.catz._
import zio.test._
import zio.test.environment.TestEnvironment
import zio.RIO

object OrganizationsEndpointTest extends DefaultRunnableSpec {
  type Task[A] = RIO[OrganizationsService with ParticipationService, A]

  override def spec: ZSpec[TestEnvironment, Any] = suite("OrganizationsEndpoint")(

    testM("should get NotFound for organization") {
      val req = Request[Task](Method.GET, uri"/organizations/20")
      checkRequest(app.run(req), Status.NotFound, Option.empty[Organization])
    },
    testM("should post and get organization") {
      val postReq = Request[Task](Method.POST, uri"/organizations")
        .withEntity(organizationForm.asJson)
      val getReq = Request[Task](Method.GET, uri"/organizations/1")
      checkRequest(app.run(postReq) *> app.run(getReq), Status.Ok, Some(organization))
    },

    testM("should join and get requests") {
      val postReq = Request[Task](Method.POST, uri"/organizations/1/join")
        .withEntity(memberRequestForm.asJson)
      val getReq = Request[Task](Method.GET, uri"/organizations/1/requests")
      checkRequest(app.run(postReq) *> app.run(getReq), Status.Ok, Some(List(memberRequest)))
    },
    testM("should approve and get participants") {
      val postReq = Request[Task](Method.POST, uri"/organizations/1/join")
        .withEntity(memberRequestForm.asJson)
      val postApproveReq = Request[Task](Method.POST, uri"/organizations/1/approve")
        .withEntity(memberApprove.asJson)
      val getReq = Request[Task](Method.GET, uri"/organizations/1/requests")
      val getPartReq = Request[Task](Method.GET, uri"/organizations/1/users")
      for {
        _ <- app.run(postReq) *> app.run(postApproveReq)
        req <- checkRequest(app.run(getReq), Status.Ok, Some(List.empty[OrgParticipationRequest]))
        users <- checkRequest(app.run(getPartReq), Status.Ok, Some(List(memberParticipation)))
      } yield req && users
    },
    testM("should leave and get participants") {
      val postPartReq = Request[Task](Method.POST, uri"/organizations/1/join")
        .withEntity(subscribeRequestForm.asJson)
      val postReq = Request[Task](Method.POST, uri"/organizations/1/join")
        .withEntity(memberRequestForm.asJson)
      val postLeaveReq = Request[Task](Method.POST, uri"/organizations/1/leave")
      val getReq = Request[Task](Method.GET, uri"/organizations/1/requests")
      val getPartReq = Request[Task](Method.GET, uri"/organizations/1/users")
      for {
        _ <- app.run(postPartReq) *> app.run(postReq) *> app.run(postLeaveReq)
        req <- checkRequest(app.run(getReq), Status.Ok, Some(List.empty[OrgParticipationRequest]))
        users <- checkRequest(app.run(getPartReq), Status.Ok, Some(List.empty[OrgUserParticipationData]))
      } yield req && users
    },

  ).provideSomeLayer(InMemoryOrganizationsService.layer ++ InMemoryParticipationService.layer(user, organization))


  private val user = User(1, "Paul", "paul@g.com", "1234", 0)
  private val userId = UserId(user.id, user.name)
  private val organization = Organization(1, "Paul Corp.", List("scala", "dev"))
  private val organizationForm = OrganizationForm(organization.name, organization.tags)

  private val subscribeRequestForm = OrgParticipationForm(OrgSubscriber)
  private val memberRequestForm = OrgParticipationForm(OrgManager)
  private val memberRequest = OrgParticipationRequest(userId, memberRequestForm.participationType, None)
  private val memberApprove = OrgParticipationApprove(user.id)
  private val memberParticipation = OrgUserParticipationData(userId, memberRequestForm.participationType)

  private val endpoint = new OrganizationsEndpoint[OrganizationsService with ParticipationService]
  private val app = endpoint.routes(alwaysAuthMiddleware(user)).orNotFound
}
