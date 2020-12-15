package tevent.user

import org.http4s.circe.CirceEntityCodec.{circeEntityDecoder, circeEntityEncoder}
import org.http4s.dsl.Http4sDsl
import org.http4s.server.{AuthMiddleware, Router}
import org.http4s.{AuthedRoutes, HttpRoutes}
import tevent.core.ErrorMapper.errorResponse
import tevent.events.dto.EventParticipationData
import tevent.events.service.Events
import tevent.organizations.dto.{OrgParticipationData, OwnOrgParticipationRequest}
import tevent.organizations.service.OrganizationParticipants
import tevent.user.dto.{LoginData, SecretForm, UserData}
import tevent.user.model.User
import tevent.user.service.{Auth, Users}
import zio._
import zio.interop.catz.taskConcurrentInstance

final class UserEndpoint[R <: Users with Auth with OrganizationParticipants with Events] {
  type Task[A] = RIO[R, A]

  private val dsl = Http4sDsl[Task]
  import dsl._

  private val httpRoutes = AuthedRoutes.of[User, Task] {
    case GET -> Root as user => Ok(UserData(user.name, user.email))
    case request@PUT -> Root as user => request.req.decode[UserData] { form =>
      Users.update(user.id, form.name, form.email).foldM(errorResponse, _ => Ok())
    }

    case POST -> Root / "revoke" as user =>
      Auth.revokeTokens(user.id).foldM(errorResponse, _ => Ok())
    case request@PUT -> Root / "secret" as user => request.req.decode[SecretForm] { form =>
      Auth.changeSecret(user.id, form.secret).foldM(
        failure = errorResponse,
        success = token => Ok(LoginData("Secret changed, use new token", token.signedString)))
    }

    case GET -> Root / "events" as user =>
      Events.getByUser(user.id).foldM(errorResponse,
        p => Ok(p.map(EventParticipationData.mapperTo)))
    case GET -> Root / "organizations" as user =>
      OrganizationParticipants.getOrganizations(user.id).foldM(errorResponse,
        p => Ok(p.map(OrgParticipationData.mapperTo)))
    case GET -> Root / "requests" as user =>
      OrganizationParticipants.getOwnRequests(user.id).foldM(errorResponse,
        r => Ok(r.map(OwnOrgParticipationRequest.mapperTo)))
  }

  def routes(implicit middleware: AuthMiddleware[Task, User]): HttpRoutes[Task] =
    Router("/user" -> httpRoutes)
}
