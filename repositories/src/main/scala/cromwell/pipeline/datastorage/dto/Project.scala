package cromwell.pipeline.datastorage.dto

import java.nio.file.Path

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
  implicit lazy val projectFormat: OFormat[Project] = Json.format[Project]
}

final case class ProjectId(value: String) extends MappedTo[String]

object ProjectId {
  implicit lazy val projectIdFormat: Format[ProjectId] = implicitly[Format[String]].inmap(ProjectId.apply, _.value)
}

final case class ProjectAdditionRequest(name: String)

final case class ProjectDeleteRequest(projectId: ProjectId)

final case class ProjectUpdateRequest(projectId: ProjectId, name: String, repository: String)

final case class Version(value: String) extends AnyVal

final case class ProjectFile(path: Path, content: String)
