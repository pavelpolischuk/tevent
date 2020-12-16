package tevent.helpers

import org.http4s.{EntityDecoder, Response, Status}
import zio.RIO
import zio.interop.catz.taskConcurrentInstance
import zio.test.Assertion.{equalTo, isEmpty}
import zio.test.AssertionM.Render.param
import zio.test.{Assertion, TestResult, assert, assertM}

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

  def hasSameMappedElements[A, B](mapping: A => B, other: Iterable[B]): Assertion[Iterable[A]] =
    Assertion.assertion("hasSameMappedElements")(param(other)) { actual =>
      val actualSeq = actual.map(mapping).toSeq
      val otherSeq  = other.toSeq

      actualSeq.diff(otherSeq).isEmpty && otherSeq.diff(actualSeq).isEmpty
    }

  def mappedAssert[A, B](mapping: A => B, assertion: Assertion[B]): Assertion[A] =
    Assertion.assertionDirect("mapped")() { actual =>
      val actualSeq = mapping(actual)
      assertion(actualSeq)
    }
}
