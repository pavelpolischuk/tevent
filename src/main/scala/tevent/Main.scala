package tevent

import tevent.main.Environments.appEnvironment
import tevent.main.Server
import zio._

object Main extends App {
  def run(args: List[String]): ZIO[ZEnv, Nothing, ExitCode] =
    Server.runServer.provideLayer(appEnvironment).exitCode
}
