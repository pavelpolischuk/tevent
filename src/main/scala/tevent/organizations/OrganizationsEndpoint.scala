package tevent.organizations

import cats.implicits.toSemigroupKOps
import org.http4s.circe.CirceEntityCodec.{circeEntityDecoder, circeEntityEncoder}
import org.http4s.dsl.Http4sDsl
import org.http4s.server.{AuthMiddleware, Router}
import org.http4s.{AuthedRoutes, HttpRoutes}
import tevent.core.ErrorMapper.errorResponse
import tevent.organizations.model.{OrgParticipation, Organization, OrganizationFilter}
import tevent.organizations.dto.OrganizationFilters.OptionalTagsQueryParamMatcher
import tevent.organizations.dto._
import tevent.organizations.service.{OrganizationParticipants, Organizations}
import tevent.user.model.User
import zio._
import zio.interop.catz.taskConcurrentInstance

final class OrganizationsEndpoint[R <: Organizations with OrganizationParticipants] {
  type Task[A] = RIO[R, A]

  private val dsl = Http4sDsl[Task]
  import dsl._

  private val httpRoutes = HttpRoutes.of[Task] {
    case GET -> Root / LongVar(id) =>
      Organizations.get(id).foldM(errorResponse, Ok(_))

    case GET -> Root
      :? OptionalTagsQueryParamMatcher(tags) => tags.map(_.toEither) match {
        case Some(Left(_)) => BadRequest("unable to parse argument tags")
        case Some(Right(tags)) => Organizations.search(OrganizationFilter(tags)).foldM(errorResponse, Ok(_))
        case None => Organizations.search(OrganizationFilter.All).foldM(errorResponse, Ok(_))
      }
  }

  private val authedRoutes = AuthedRoutes.of[User, Task] {
    case GET -> Root / LongVar(id) / "requests" as user =>
      OrganizationParticipants.getRequests(user.id, id).foldM(errorResponse,
        r => Ok(r.map(OrgParticipationRequest.mapperTo)))
    case GET -> Root / LongVar(id) / "users" as user =>
      OrganizationParticipants.getUsers(user.id, id).foldM(errorResponse,
        r => Ok(r.map(OrgUserParticipationData.mapperTo)))

    case request@POST -> Root as user => request.req.decode[OrganizationForm] { form =>
      Organizations.create(user.id, Organization(-1, form.name, form.nick, form.description, form.tags)).foldM(errorResponse, Ok(_))
    }
    case request@PUT -> Root / LongVar(id) as user => request.req.decode[OrganizationForm] { form =>
      Organizations.update(user.id, Organization(id, form.name, form.nick, form.description, form.tags)).foldM(errorResponse, Ok(_))
    }

    case request@POST -> Root / LongVar(id) / "join" as user => request.req.decode[OrgParticipationForm] { form =>
      val r = OrgParticipation(user.id, id, form.participationType)
      OrganizationParticipants.joinOrganization(r).foldM(errorResponse, Ok(_))
    }
    case request@POST -> Root / LongVar(id) / "invite" as user => request.req.decode[OrgParticipationInvite] { form =>
      val invite = OrgParticipation(form.userId, id, form.participationType)
      OrganizationParticipants.inviteIntoOrganization(invite, user.id).foldM(errorResponse, Ok(_))
    }
    case request@POST -> Root / LongVar(id) / "approve" as user => request.req.decode[OrgParticipationApprove] { form =>
      OrganizationParticipants.approveRequest(form.userId, id, user.id).foldM(errorResponse, Ok(_))
    }
    case POST -> Root / LongVar(id) / "leave" as user =>
      OrganizationParticipants.leaveOrganization(user.id, id).foldM(errorResponse, Ok(_))
  }

  def routes(implicit middleware: AuthMiddleware[Task, User]): HttpRoutes[Task] =
    Router("/organizations" -> (httpRoutes <+> middleware(authedRoutes)))
}
