package tevent.events

import zio.Has

package object service {
  type Events = Has[Events.Service]
}
