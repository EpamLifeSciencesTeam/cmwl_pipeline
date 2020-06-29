package cromwell.pipeline.utils

import akka.http.scaladsl.server.Rejection

final case class MissingAccessTokenRejection(msg: String) extends Rejection
