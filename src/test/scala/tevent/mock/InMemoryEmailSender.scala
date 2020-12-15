package tevent.mock

import tevent.notification.Email
import zio.{Ref, _}

class InMemoryEmailSender(ref: Ref[List[String]]) extends Email.Service {

  override def sendMail(receiver: String, subject: String, content: String): Task[Unit] =
    ref.update(s => s :+ receiver)
}

object InMemoryEmailSender {
  def layer(receivers: Ref[List[String]]): ULayer[Email] =
    ZLayer.succeed(new InMemoryEmailSender(receivers))
}
