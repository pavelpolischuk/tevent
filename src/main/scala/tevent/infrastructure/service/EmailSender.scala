package tevent.infrastructure.service

import courier.{Envelope, Mailer, Text, addr}
import tevent.infrastructure.Configuration.{Config, GmailConfig}
import zio.{RIO, Task, ZIO}

import javax.mail.internet.InternetAddress

object EmailSender {
  trait Service {
    def sendMail(receiver: String, subject: String, content: String): Task[Unit]
  }

  class GmailSender(config: GmailConfig) extends EmailSender.Service {

    private val mailer = Mailer("smtp.gmail.com", 465)
      .auth(true)
      .as(s"${config.sender}@gmail.com", config.secret)
      .ssl(true)()

    override def sendMail(receiver: String, subject: String, content: String): Task[Unit] = Task.fromFuture { ec =>
      val envelope = Envelope.from(config.sender at "gmail.com")
        .to(new InternetAddress(receiver))
        .subject(subject)
        .content(Text(content))

      mailer(envelope)(ec)
    }
  }

  val live: ZIO[Config, Nothing, GmailSender] =
    ZIO.access[Config](_.get[GmailConfig]).map(c => new GmailSender(c))


  def sendMail(receiver: String, subject: String, content: String): RIO[Email, Unit] =
    ZIO.accessM(_.get.sendMail(receiver, subject, content))
}