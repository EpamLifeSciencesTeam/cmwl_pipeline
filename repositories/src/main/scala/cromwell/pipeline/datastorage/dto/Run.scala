package cromwell.pipeline.datastorage.dto

import java.time.Instant

import cromwell.pipeline.model.wrapper.{ RunId, UserId }
import play.api.libs.functional.syntax._
import play.api.libs.json._

final case class Run(
  runId: RunId,
  projectId: ProjectId,
  projectVersion: String,
  status: Status = Created,
  timeStart: Instant,
  timeEnd: Option[Instant] = None,
  userId: UserId,
  results: String,
  cmwlWorkflowId: Option[String] = None
)

object Run {
  implicit lazy val runFormat: OFormat[Run] = Json.format[Run]
}

sealed trait Status
case object Created extends Status
case object Submitted extends Status
case object Done extends Status

object Status {
  implicit lazy val statusFormat: Format[Status] =
    implicitly[Format[String]].inmap(Status.fromString, Status.toString)

  def fromString(s: String): Status = s match {
    case "submitted" => Submitted
    case "done"      => Done
    case "created"   => Created
  }

  def toString(status: Status): String = status match {
    case Submitted => "submitted"
    case Done      => "done"
    case Created   => "created"
  }

  def values = Seq(Submitted, Done, Created)
}

final case class RunCreateRequest(
  projectId: ProjectId,
  projectVersion: String,
  results: String,
  userId: UserId,
  cmwlWorkflowId: Option[String] = None
)

object RunCreateRequest {
  implicit val updateRequestFormat: OFormat[RunCreateRequest] = Json.format[RunCreateRequest]
}

final case class RunDeleteRequest(runId: RunId)

object RunDeleteRequest {
  implicit lazy val projectDeleteFormat: OFormat[RunDeleteRequest] = Json.format[RunDeleteRequest]
}

final case class RunUpdateRequest(
  runId: RunId,
  status: Status,
  timeStart: Instant,
  timeEnd: Option[Instant],
  results: String,
  cmwlWorkflowId: Option[String]
)

object RunUpdateRequest {
  implicit val updateRequestFormat: OFormat[RunUpdateRequest] = Json.format[RunUpdateRequest]
}
