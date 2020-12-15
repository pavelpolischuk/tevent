package tevent.helpers

import cats.data.{Kleisli, OptionT}
import org.http4s._
import org.http4s.dsl.Http4sDsl
import org.http4s.server.AuthMiddleware
import tevent.user.model.User
import zio.interop.catz.taskConcurrentInstance
import zio.{RIO, Task}


object AlwaysAuthMiddleware {
  def apply[R](user: User): AuthMiddleware[RIO[R, *], User] = {
    type Task[A] = RIO[R, A]
    val dsl = Http4sDsl[Task]
    import dsl._

    def authUser: Kleisli[Task, Request[Task], Either[Nothing, User]] = Kleisli({ _ => Task.right(user)})
    def onFailure: AuthedRoutes[Nothing, Task] = Kleisli(_ => OptionT.liftF(Forbidden("")))

    AuthMiddleware(authUser, onFailure)
  }
}
