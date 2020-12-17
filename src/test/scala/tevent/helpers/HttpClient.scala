package tevent.helpers

import cats.effect.Blocker
import io.circe.{Decoder, Encoder}
import org.http4s.Method.{DELETE, GET, POST, PUT}
import org.http4s.client.middleware.{RequestLogger, ResponseLogger}
import org.http4s.client.{Client, JavaNetClientBuilder}
import org.http4s.headers.Authorization
import org.http4s._
import org.http4s.circe.CirceEntityCodec.circeEntityEncoder
import org.http4s.circe.jsonOf
import zio.Task
import zio.interop.catz.{taskConcurrentInstance, zioContextShift}

import java.util.concurrent.Executors

object HttpClient {

  private val blockingPool = Executors.newFixedThreadPool(1)
  private val blocker = Blocker.liftExecutorService(blockingPool)

  private val httpClient: Client[Task] = JavaNetClientBuilder[Task](blocker).create
  private val loggingClient: Client[Task] = RequestLogger(logHeaders = true, logBody = true)(
    ResponseLogger(logHeaders = false, logBody = true)(httpClient))

  private def expect[U: Encoder, R: Decoder](method: Method, uri: String, data: U): Task[R] =
    loggingClient.expect[R](
      Request[Task](method, Uri.unsafeFromString(uri))
        .withEntity(data)
    )(jsonOf[Task, R])

  private def expect[U: Encoder, R: Decoder](method: Method, uri: String, token: String, data: U): Task[R] =
    loggingClient.expect[R](
      Request[Task](method, Uri.unsafeFromString(uri),
        headers = Headers.of(Authorization(Credentials.Token(AuthScheme.Bearer, token))))
        .withEntity(data)
    )(jsonOf[Task, R])

  def post[U: Encoder, R: Decoder](uri: String, data: U): Task[R] = expect(POST, uri, data)

  def post[U: Encoder, R: Decoder](uri: String, token: String, data: U): Task[R] = expect(POST, uri, token, data)

  def put[U: Encoder, R: Decoder](uri: String, data: U): Task[R] = expect(PUT, uri, data)

  def put[U: Encoder, R: Decoder](uri: String, token: String, data: U): Task[R] = expect(PUT, uri, token, data)

  def delete(uri: String, token: String): Task[Unit] =
    loggingClient.expect[Unit](
      Request[Task](DELETE, Uri.unsafeFromString(uri),
        headers = Headers.of(Authorization(Credentials.Token(AuthScheme.Bearer, token)))
      ))

  def get[R: Decoder](uri: String): Task[R] =
    loggingClient.expect[R](
      Request[Task](GET, Uri.unsafeFromString(uri))
    )(jsonOf[Task, R])

  def get[R: Decoder](uri: String, token: String): Task[R] =
    loggingClient.expect[R](
      Request[Task](GET, Uri.unsafeFromString(uri))
        .withHeaders(Authorization(Credentials.Token(AuthScheme.Bearer, token)))
    )(jsonOf[Task, R])
}
