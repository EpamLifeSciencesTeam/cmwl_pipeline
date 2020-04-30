package cromwell.pipeline.datastorage.dto

import java.nio.file.{ Path, Paths }

import play.api.libs.functional.syntax._
import play.api.libs.json._
import slick.lifted.MappedTo

final case class Project(
  projectId: ProjectId,
  ownerId: UserId,
  name: String,
  active: Boolean,
  repository: Option[Repository] = None,
  visibility: Visibility = Private
) {
  def withRepository(repositoryPath: Option[String]): Project =
    this.copy(repository = repositoryPath.map(Repository(_)))
}
object Project {
  implicit lazy val projectFormat: OFormat[Project] = Json.format[Project]
  implicit lazy val projectWrites: Writes[Project] = (project: Project) =>
    Json.obj(
      "projectId" -> project.projectId.value,
      "ownerId" -> project.ownerId,
      "name" -> project.name,
      "active" -> project.active,
      "repository" -> project.repository,
      "visibility" -> Visibility.toString(project.visibility),
      "path" -> project.projectId.value
    )
}

final case class ProjectId(value: String) extends MappedTo[String]

object ProjectId {
  implicit lazy val projectIdFormat: Format[ProjectId] = implicitly[Format[String]].inmap(ProjectId.apply, _.value)
}

final case class Repository(value: String) extends MappedTo[String]

object Repository {
  implicit lazy val repositoryFormat: Format[Repository] = implicitly[Format[String]].inmap(Repository.apply, _.value)
}

final case class ProjectAdditionRequest(name: String)

object ProjectAdditionRequest {
  implicit lazy val projectAdditionFormat: OFormat[ProjectAdditionRequest] = Json.format[ProjectAdditionRequest]
}

final case class ProjectDeleteRequest(projectId: ProjectId)

object ProjectDeleteRequest {
  implicit lazy val projectDeleteFormat: OFormat[ProjectDeleteRequest] = Json.format[ProjectDeleteRequest]
}

final case class ProjectUpdateRequest(projectId: ProjectId, name: String, repository: Option[Repository])

object ProjectUpdateRequest {
  implicit val updateRequestFormat: OFormat[ProjectUpdateRequest] = Json.format[ProjectUpdateRequest]
}

final case class Version(name: String, message: String, target: String, commit: Commit)
object Version {
  implicit val versionPlayFormat: OFormat[Version] = Json.format[Version]
}

final case class Commit(id: String)
object Commit {
  implicit val commitFormat: OFormat[Commit] = Json.format[Commit]
}

final case class ProjectFile(path: Path, content: String)

object ProjectFile {
  implicit object ProjectFileFormat extends Format[ProjectFile] {
    override def reads(json: JsValue): JsResult[ProjectFile] =
      JsSuccess(ProjectFile(Paths.get((json \ "path").as[String]), (json \ "content").as[String]))

    override def writes(o: ProjectFile): JsValue = JsObject(
      Seq("path" -> JsString(o.path.toString), "content" -> JsString(o.content))
    )
  }
}

sealed trait Visibility
case object Private extends Visibility
case object Internal extends Visibility
case object Public extends Visibility

object Visibility {
  implicit lazy val visibilityFormat: Format[Visibility] =
    implicitly[Format[String]].inmap(Visibility.fromString, Visibility.toString)

  def fromString(s: String): Visibility = s match {
    case "private"  => Private
    case "internal" => Internal
    case "public"   => Public
  }

  def toString(visibility: Visibility): String = visibility match {
    case Private  => "private"
    case Internal => "internal"
    case Public   => "public"
  }

  def values = Seq(Private, Internal, Public)
}

final case class FileContent(content: String)

object FileContent {
  implicit lazy val validateFileRequestFormat: OFormat[FileContent] = Json.format[FileContent]
}

final case class ProjectUpdateFileRequest(project: Project, projectFile: ProjectFile)

object ProjectUpdateFileRequest {
  implicit lazy val projectUpdateFileRequestFormat: OFormat[ProjectUpdateFileRequest] =
    Json.format[ProjectUpdateFileRequest]
}
