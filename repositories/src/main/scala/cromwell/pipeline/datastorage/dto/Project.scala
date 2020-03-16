package cromwell.pipeline.datastorage.dto

import java.nio.file.Path

import play.api.libs.json.{ Format, Json, OFormat }
import slick.lifted.MappedTo
import play.api.libs.functional.syntax._

final case class Project(
  projectId: ProjectId,
  ownerId: UserId,
  name: String,
  repository: String,
  active: Boolean
)

object Project {
  implicit lazy val projectFormat: OFormat[Project] = Json.format[Project]
}

final case class ProjectId(value: String) extends MappedTo[String]

object ProjectId {
  implicit lazy val projectIdFormat: Format[ProjectId] = implicitly[Format[String]].inmap(ProjectId.apply, _.value)
}

final case class ProjectAdditionRequest(name: String)

object ProjectAdditionRequest {
  implicit lazy val projectAdditionFormat: OFormat[ProjectAdditionRequest] = Json.format[ProjectAdditionRequest]
}

final case class ProjectDeleteRequest(projectId: ProjectId)

object ProjectDeleteRequest {
  implicit lazy val projectDeleteFormat: OFormat[ProjectDeleteRequest] = Json.format[ProjectDeleteRequest]
}

final case class ProjectUpdateRequest(projectId: ProjectId, name: String, repository: String)

object ProjectUpdateRequest {
  implicit val updateRequestFormat: OFormat[ProjectUpdateRequest] = Json.format[ProjectUpdateRequest]
}

final case class Version(value: String) extends AnyVal

final case class ProjectFile(path: Path, content: String)
