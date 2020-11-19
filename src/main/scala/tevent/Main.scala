package tevent

import tevent.http.Server
import tevent.infrastructure.Environments.appEnvironment
import zio._

object Main extends App {
  def run(args: List[String]): ZIO[ZEnv, Nothing, ExitCode] =
    Server.runServer.provideLayer(appEnvironment).exitCode
}
