package test

import org.mashupbots.socko.events.HttpRequestEvent
import akka.actor.Actor
import java.util.Date

/**
 * Returns dynamic content
 */
class DynamicBenchmarkHandler extends Actor {
  def receive = {
    case event: HttpRequestEvent =>
      val content = "<html>\n<body>\nDate and time is " + new Date().toString + "\n</body>\n</html>\n"
      event.response.write(content, "text/html; charset=UTF-8")
      //context.stop(self)
  }
}