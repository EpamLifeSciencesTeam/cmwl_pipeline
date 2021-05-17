package cromwell.pipeline.utils

import scala.concurrent.duration.{ Duration, DurationInt }

trait TestTimeout {

  implicit val timeoutAsDuration: Duration = 30.seconds

}
