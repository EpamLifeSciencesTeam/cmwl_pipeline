package cromwell.pipeline.auth.token

import akka.http.scaladsl.server.Rejection

final case class MissingAccessTokenRejection(msg: String) extends Rejection
