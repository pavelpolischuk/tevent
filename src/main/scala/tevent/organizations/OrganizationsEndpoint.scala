package tevent.organizations

import cats.implicits.toSemigroupKOps
import org.http4s.circe.CirceEntityCodec.{circeEntityDecoder, circeEntityEncoder}
import org.http4s.dsl.Http4sDsl
import org.http4s.server.{AuthMiddleware, Router}
import org.http4s.{AuthedRoutes, HttpRoutes}
import sttp.tapir.EndpointIO.Example
import sttp.tapir._
import sttp.tapir.json.circe._
import sttp.tapir.generic.auto._
import tevent.core.ErrorMapper.errorResponse
import tevent.organizations.model.{OrgMember, OrgOwner, OrgSubscriber, OrgManager, OrgParticipation, Organization, OrganizationFilter}
import tevent.organizations.dto.OrganizationFilters.OptionalTagsQueryParamMatcher
import tevent.organizations.dto._
import tevent.organizations.service.{OrganizationParticipants, Organizations}
import tevent.user.model.{User, UserId}
import zio._
import zio.interop.catz.taskConcurrentInstance

final class OrganizationsEndpoint[R <: Organizations with OrganizationParticipants] {
  type Task[A] = RIO[R, A]

  private val dsl = Http4sDsl[Task]
  import dsl._

  private val httpRoutes = HttpRoutes.of[Task] {
    case GET -> Root / OrganizationIdVar(id) =>
      Organizations.get(id).foldM(errorResponse, Ok(_))

    case GET -> Root
      :? OptionalTagsQueryParamMatcher(tags) => tags.map(_.toEither) match {
        case Some(Left(_)) => BadRequest("unable to parse argument tags")
        case Some(Right(tags)) => Organizations.search(OrganizationFilter(tags)).foldM(errorResponse, Ok(_))
        case None => Organizations.search(OrganizationFilter.All).foldM(errorResponse, Ok(_))
      }
  }

  private val authedRoutes = AuthedRoutes.of[User, Task] {
    case GET -> Root / OrganizationIdVar(id) / "requests" as user =>
      OrganizationParticipants.getRequests(user.typedId, id).foldM(errorResponse,
        r => Ok(r.map(OrgParticipationRequest.mapperTo)))
    case GET -> Root / OrganizationIdVar(id) / "users" as user =>
      OrganizationParticipants.getUsers(user.typedId, id).foldM(errorResponse,
        r => Ok(r.map(OrgUserParticipationData.mapperTo)))

    case request@POST -> Root as user => request.req.decode[OrganizationForm] { form =>
      Organizations.create(user.typedId, Organization(-1, form.name, form.nick, form.description, form.tags)).foldM(errorResponse, Ok(_))
    }
    case request@PUT -> Root / OrganizationIdVar(id) as user => request.req.decode[OrganizationForm] { form =>
      Organizations.update(user.typedId, Organization(id.id, form.name, form.nick, form.description, form.tags)).foldM(errorResponse, Ok(_))
    }

    case request@POST -> Root / OrganizationIdVar(id) / "join" as user => request.req.decode[OrgParticipationForm] { form =>
      val r = OrgParticipation(user.id, id.id, form.participationType)
      OrganizationParticipants.joinOrganization(r).foldM(errorResponse, Ok(_))
    }
    case request@POST -> Root / OrganizationIdVar(id) / "invite" as user => request.req.decode[OrgParticipationInvite] { form =>
      val invite = OrgParticipation(form.userId, id.id, form.participationType)
      OrganizationParticipants.inviteIntoOrganization(invite, user.typedId).foldM(errorResponse, Ok(_))
    }
    case request@POST -> Root / OrganizationIdVar(id) / "approve" as user => request.req.decode[OrgParticipationApprove] { form =>
      OrganizationParticipants.approveRequest(UserId(form.userId), id, user.typedId).foldM(errorResponse, Ok(_))
    }
    case POST -> Root / OrganizationIdVar(id) / "leave" as user =>
      OrganizationParticipants.leaveOrganization(user.typedId, id).foldM(errorResponse, Ok(_))
  }

  def routes(implicit middleware: AuthMiddleware[Task, User]): HttpRoutes[Task] =
    Router("/organizations" -> (httpRoutes <+> middleware(authedRoutes)))

  def docRoutes(basePath: String): List[Endpoint[_, _, _, _]] = {

    val organizationId = path[Long]("organizationId")

    val organizationForm = jsonBody[OrganizationForm]
      .description("The organization data")
      .example(OrganizationForm("Organization #1", "org1", "Org description", List("tag1")))

    val authHeader = auth.bearer[String]()

    val getOrg = endpoint.get
      .in(basePath / "organizations")
      .in(organizationId)
      .out(jsonBody[Organization])

    val getOrgList = endpoint.get
      .in(basePath / "organizations")
      .in(query[Option[String]]("tags")
        .description("Tags to search")
        .example(Some("it+dev+scala")))
      .out(jsonBody[List[Organization]])

    val getRequests = endpoint.get
      .in(basePath / "organizations")
      .in(organizationId)
      .in("requests")
      .in(authHeader)
      .out(jsonBody[List[OrgParticipationRequest]])

    val getUsers = endpoint.get
      .in(basePath / "organizations")
      .in(organizationId)
      .in("users")
      .in(authHeader)
      .out(jsonBody[List[OrgUserParticipationData]])

    val postOrg = endpoint.post
      .in(basePath / "organizations")
      .in(authHeader)
      .in(organizationForm)
      .out(jsonBody[Organization])

    val putOrg = endpoint.put
      .in(basePath / "organizations")
      .in(organizationId)
      .in(authHeader)
      .in(organizationForm)

    val postJoin = endpoint.post
      .in(basePath / "organizations")
      .in(organizationId)
      .in("join")
      .in(authHeader)
      .in(jsonBody[OrgParticipationForm]
        .examples(List(OrgSubscriber, OrgMember, OrgManager, OrgOwner).map(t => Example.of(OrgParticipationForm(t), Some(t.toString)))))

    val postInvite = endpoint.post
      .in(basePath / "organizations")
      .in(organizationId)
      .in("invite")
      .in(authHeader)
      .in(jsonBody[OrgParticipationInvite]
        .examples(List(OrgSubscriber, OrgMember, OrgManager, OrgOwner).map(t => Example.of(OrgParticipationInvite(42, t), Some(t.toString)))))

    val postApprove = endpoint.post
      .in(basePath / "organizations")
      .in(organizationId)
      .in("approve")
      .in(authHeader)
      .in(jsonBody[OrgParticipationApprove]
        .example(OrgParticipationApprove(42)))

    val postLeave = endpoint.post
      .in(basePath / "organizations")
      .in(organizationId)
      .in("leave")
      .in(authHeader)

    List(getOrg, getOrgList, getRequests, getUsers, postOrg, putOrg, postJoin, postInvite, postApprove, postLeave)
  }
}
