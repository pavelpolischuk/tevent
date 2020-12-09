package tevent

import cats.data.{Kleisli, OptionT}
import org.http4s.dsl.Http4sDsl
import org.http4s.server.AuthMiddleware
import org.http4s._
import tevent.domain.model.User
import zio.interop.catz.taskConcurrentInstance
import zio.test.Assertion.{equalTo, isEmpty}
import zio.test.{TestResult, assert, assertM}
import zio.{RIO, Task}

package object route {
  def alwaysAuthMiddleware[R](user: User): AuthMiddleware[RIO[R, *], User] = {
    type Task[A] = RIO[R, A]
    val dsl = Http4sDsl[Task]
    import dsl._

    def authUser: Kleisli[Task, Request[Task], Either[Nothing, User]] = Kleisli({ _ => Task.right(user)})
    def onFailure: AuthedRoutes[Nothing, Task] = Kleisli(_ => OptionT.liftF(Forbidden("")))

    AuthMiddleware(authUser, onFailure)
  }

  def checkRequest[R, A](actual: RIO[R, Response[RIO[R, *]]], expectedStatus: Status, expectedBody: Option[A]
                        )(implicit ev: EntityDecoder[RIO[R, *], A]): RIO[R, TestResult] =
    for {
      actual <- actual
      bodyResult <- expectedBody.fold[RIO[R, TestResult]](
        assertM(actual.bodyText.compile.toVector)(isEmpty))(
        expected => assertM(actual.as[A])(equalTo(expected)))
      statusResult = assert(actual.status)(equalTo(expectedStatus))
    } yield bodyResult && statusResult
}
