package tevent

import zio.Has

package object notification {
  type Email = Has[Email.Service]
  type Notification = Has[Notification.Service]
}
