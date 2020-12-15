package tevent.helpers

import org.http4s.{EntityDecoder, Response, Status}
import zio.RIO
import zio.interop.catz.taskConcurrentInstance
import zio.test.Assertion.{equalTo, isEmpty}
import zio.test.{TestResult, assert, assertM}

object TestHelper {
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
