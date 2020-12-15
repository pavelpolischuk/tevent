package tevent.notification

import courier.{Envelope, Mailer, Text, addr}
import tevent.core.Config
import tevent.core.Config.GmailConfig
import zio.{RIO, Task, URLayer, ZIO}

import javax.mail.internet.InternetAddress

object Email {
  trait Service {
    def sendMail(receiver: String, subject: String, content: String): Task[Unit]
  }

  class GmailSender(config: GmailConfig) extends Email.Service {

    private val mailer = Mailer("smtp.gmail.com", 465)
      .auth(true)
      .as(s"${config.sender}@gmail.com", config.secret)
      .ssl(true)()

    override def sendMail(receiver: String, subject: String, content: String): Task[Unit] = Task.fromFuture { ec =>
      val envelope = Envelope.from(config.sender.at("gmail.com"))
        .to(new InternetAddress(receiver))
        .subject(subject)
        .content(Text(content))

      mailer(envelope)(ec)
    }
  }

  val live: URLayer[Config, Email] =
    ZIO.service[GmailConfig].map(c => new GmailSender(c)).toLayer

  val option: URLayer[Config, Email] =
    ZIO.service[GmailConfig].map(c =>
      if (c.sender.isEmpty) new Service {
        override def sendMail(receiver: String, subject: String, content: String): Task[Unit] = Task.unit
      }
      else new GmailSender(c)
    ).toLayer

  def sendMail(receiver: String, subject: String, content: String): RIO[Email, Unit] =
    ZIO.accessM(_.get.sendMail(receiver, subject, content))
}
