package tevent.mock

import tevent.notification.Email
import zio.test.mock
import zio.test.mock.{Expectation, Mock}
import zio.{Has, Task, URLayer, ZLayer}

object EmailMock extends Mock[Email] {
  object SendMail extends Effect[(String, String, String), Throwable, Unit]

  private object empty extends Effect[Unit, Nothing, Unit]

  val Empty: Expectation[Email] = empty().optional

  override val compose: URLayer[Has[mock.Proxy], Email] = ZLayer.fromServiceM { proxy =>
    withRuntime.as {
      new Email.Service {
        override def sendMail(receiver: String, subject: String, content: String): Task[Unit] = proxy(SendMail, receiver, subject, content)
      }
    }
  }
}

