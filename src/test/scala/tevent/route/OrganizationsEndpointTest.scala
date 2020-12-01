package tevent.route

import org.http4s.implicits.{http4sKleisliResponseSyntaxOptionT, http4sLiteralsSyntax}
import org.http4s.{Method, Request, Status}
import io.circe.syntax.EncoderOps
import org.http4s.circe.jsonEncoder
import tevent.domain.DomainError
import tevent.domain.model._
import tevent.http.endpoints.{OrganizationsEndpoint, circeJsonDecoder}
import tevent.http.model.organization.{OrgParticipationApprove, OrgParticipationForm, OrgParticipationRequest, OrgUserParticipationData, OrganizationForm}
import tevent.http.model.user.UserId
import tevent.mock.InMemoryOrganizationsService
import tevent.service.{OrganizationsService, ParticipationService}
import zio.interop.catz._
import zio.test._
import zio.test.environment.TestEnvironment
import zio.{IO, RIO, ULayer, ZLayer}

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

  ).provideSomeLayer(InMemoryOrganizationsService.layer ++ participationService)


  private val user = User(1, "Paul", "paul@g.com", "1234")
  private val userId = UserId(user.id, user.name)
  private val organization = Organization(1, "Paul Corp.")
  private val organizationForm = OrganizationForm(organization.name)

  private val ownerParticipation = OrgUserParticipationData(userId, OrgOwner)
  private val memberRequestForm = OrgParticipationForm(OrgManager)
  private val memberRequest = OrgParticipationRequest(userId, memberRequestForm.participationType, None)
  private val memberApprove = OrgParticipationApprove(user.id)
  private val memberParticipation = OrgUserParticipationData(userId, memberRequestForm.participationType)

  def participationService: ULayer[ParticipationService] = ZLayer.succeed(new ParticipationService.Service {
    override def checkUser(userId: Long, organizationId: Long, role: OrgParticipationType): IO[DomainError, Unit] = ???

    override def getUsers(userId: Long, organizationId: Long): IO[DomainError, List[(User, OrgParticipationType)]] = ???

    override def getOrganizations(userId: Long): IO[DomainError, List[(Organization, OrgParticipationType)]] = ???

    override def getRequests(userId: Long, organizationId: Long): IO[DomainError, List[(User, OrgParticipationType, User)]] = ???

    override def getOwnRequests(userId: Long): IO[DomainError, List[(Organization, OrgParticipationType, Option[User])]] = ???

    override def joinOrganization(participation: OrgParticipation): IO[DomainError, Unit] = ???

    override def inviteIntoOrganization(participation: OrgParticipation, inviterId: Long): IO[DomainError, Unit] = ???

    override def approveRequestIntoOrganization(userId: Long, organizationId: Long, approverId: Long): IO[DomainError, Unit] = ???

    override def leaveOrganization(userId: Long, organizationId: Long): IO[DomainError, Unit] = ???
  })

  private val endpoint = new OrganizationsEndpoint[OrganizationsService with ParticipationService]
  private val app = endpoint.routes(alwaysAuthMiddleware(user)).orNotFound
}
