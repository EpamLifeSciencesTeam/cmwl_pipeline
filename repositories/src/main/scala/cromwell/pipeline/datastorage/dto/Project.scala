package cromwell.pipeline.datastorage.dto

import java.nio.file.Path

import play.api.libs.functional.syntax._
import play.api.libs.json._
import slick.lifted.MappedTo

final case class Project(
  projectId: ProjectId,
  ownerId: UserId,
  name: String,
  repository: String,
  visibility: Visibility = Private,
  active: Boolean
)
object Project {
  implicit lazy val projectFormat: OFormat[Project] = Json.format[Project]
  implicit lazy val projectWrites: Writes[Project] = (project: Project) =>
    Json.obj(
      "projectId" -> project.projectId.value,
      "ownerId" -> project.ownerId,
      "name" -> project.name,
      "repository" -> project.repository,
      "visibility" -> Visibility.fromVisibility(project.visibility),
      "active" -> project.active,
      "path" -> project.projectId.value
    )
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

sealed trait Visibility
case object Private extends Visibility
case object Internal extends Visibility
case object Public extends Visibility
object Visibility {
  def toVisibility(string: String): Visibility = string match {
    case "private"  => Private
    case "internal" => Internal
    case "public"   => Public
  }
  def fromVisibility(visibility: Visibility): String = visibility match {
    case Private  => "private"
    case Internal => "internal"
    case Public   => "public"
  }
  implicit lazy val projectVisibilityFormat: Format[Visibility] =
    implicitly[Format[String]].inmap(toVisibility, fromVisibility)

}
