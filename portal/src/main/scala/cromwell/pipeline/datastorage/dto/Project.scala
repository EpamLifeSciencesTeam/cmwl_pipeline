package cromwell.pipeline.datastorage.dto

import play.api.libs.functional.syntax._
import play.api.libs.json.{ Format, Json, OFormat }
import slick.lifted.MappedTo

final case class Project(
  projectId: ProjectId,
  ownerId: UserId,
  name: String,
  repository: String,
  active: Boolean
)
object Project {
  implicit val projectFormat: OFormat[Project] = Json.format[Project]
}

final case class ProjectId(value: String) extends MappedTo[String]
object ProjectId {
  implicit val projectIdFormat: Format[ProjectId] = implicitly[Format[String]].inmap(ProjectId.apply, _.value)
}

final case class ProjectCreationRequest(ownerId: UserId, name: String, repository: String)
object ProjectCreationRequest {
  implicit val projectCreationRequestFormat: OFormat[ProjectCreationRequest] = Json.format[ProjectCreationRequest]
}
