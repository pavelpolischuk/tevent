package tevent.http.endpoints

import org.http4s.dsl.Http4sDsl
import org.http4s.server.{AuthMiddleware, Router}
import org.http4s.{AuthedRoutes, HttpRoutes}
import tevent.domain.model.{OrgParticipation, User}
import tevent.http.model.organization._
import tevent.service.{EventsService, OrganizationsService, ParticipationService}
import zio._
import zio.interop.catz.taskConcurrentInstance

final class OrganizationsEndpoint[R <: OrganizationsService with EventsService with ParticipationService] {
  type Task[A] = RIO[R, A]

  private val prefixPath = "/organizations"
  private val dsl = Http4sDsl[Task]
  import dsl._

  private val httpRoutes = AuthedRoutes.of[User, Task] {
    case GET -> Root / LongVar(id) as user =>
      OrganizationsService.get(id).foldM(errorMapper, _.fold(NotFound())(Ok(_)))
    case GET -> Root / LongVar(id) / "events" as user =>
      EventsService.getByOrganization(id).foldM(errorMapper, Ok(_))
    case GET -> Root / LongVar(id) / "requests" as user =>
      ParticipationService.getRequests(user.id, id).foldM(errorMapper,
        r => Ok(r.map(OrgParticipationRequest.mapperTo)))
    case GET -> Root / LongVar(id) / "users" as user =>
      ParticipationService.getUsers(user.id, id).foldM(errorMapper,
        r => Ok(r.map(OrgUserParticipationData.mapperTo)))

    case request@POST -> Root as user => request.req.decode[OrganizationForm] { form =>
      OrganizationsService.create(user.id, form.name).foldM(errorMapper, Ok(_))
    }

    case request@POST -> Root / LongVar(id) / "join" as user => request.req.decode[OrgParticipationForm] { form =>
      val r = OrgParticipation(user.id, id, form.participationType)
      ParticipationService.joinOrganization(r).foldM(errorMapper, Ok(_))
    }
    case request@POST -> Root / LongVar(id) / "invite" as user => request.req.decode[OrgParticipationInvite] { form =>
      val invite = OrgParticipation(form.userId, id, form.participationType)
      ParticipationService.inviteIntoOrganization(invite, user.id).foldM(errorMapper, Ok(_))
    }
    case request@POST -> Root / LongVar(id) / "approve" as user => request.req.decode[OrgParticipationApprove] { form =>
      ParticipationService.approveRequestIntoOrganization(form.userId, id, user.id).foldM(errorMapper, Ok(_))
    }
    case POST -> Root / LongVar(id) / "leave" as user =>
      ParticipationService.leaveOrganization(user.id, id).foldM(errorMapper, Ok(_))
  }

  def routes(implicit middleware: AuthMiddleware[Task, User]): HttpRoutes[Task] = Router(
    prefixPath -> httpRoutes
  )
}
