package tevent

import zio.Has

package object core {
  type Config = Has[Config.DbConfig] with Has[Config.HttpServerConfig] with Has[Config.GmailConfig]
  type Db = Has[Db.Service]
}
