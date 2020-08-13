package cromwell.pipeline.datastorage.formatters

import java.nio.file.{ Path, Paths }

import cromwell.pipeline.datastorage.dto.File.UpdateFileRequest
import cromwell.pipeline.datastorage.dto._
import play.api.libs.functional.syntax._
import play.api.libs.json._
import cromwell.pipeline.datastorage.formatters.FileFormatters._

object ProjectFormatters {
  implicit object pathFormat extends Format[Path] {
    override def writes(o: Path): JsValue = JsString(o.toString)
    override def reads(json: JsValue): JsResult[Path] =
      json.validate[String].map(s => Paths.get(s))
  }
  implicit lazy val projectFileFormat: OFormat[ProjectFile] = {
    ((JsPath \ "path").format[Path] ~ (JsPath \ "content")
      .format[ProjectFileContent])(ProjectFile.apply, unlift(ProjectFile.unapply))
  }
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
  implicit lazy val commitFormat: OFormat[Commit] = Json.format[Commit]
  implicit lazy val fileCommitFormat: OFormat[FileCommit] = Json.format[FileCommit]
  implicit lazy val versionPlayFormat: OFormat[PipelineVersion] = Json.format[PipelineVersion]
  implicit lazy val visibilityFormat: Format[Visibility] =
    implicitly[Format[String]].inmap(Visibility.fromString, Visibility.toString)
  implicit lazy val projectIdFormat: Format[ProjectId] = implicitly[Format[String]].inmap(ProjectId.apply, _.value)
  implicit lazy val repositoryFormat: Format[Repository] = implicitly[Format[String]].inmap(Repository.apply, _.value)
  implicit lazy val projectAdditionFormat: OFormat[ProjectAdditionRequest] = Json.format[ProjectAdditionRequest]
  implicit lazy val projectDeleteFormat: OFormat[ProjectDeleteRequest] = Json.format[ProjectDeleteRequest]
  implicit lazy val projectUpdateRequestFormat: OFormat[ProjectUpdateRequest] = Json.format[ProjectUpdateRequest]
  implicit lazy val validateFileRequestFormat: OFormat[ProjectFileContent] = Json.format[ProjectFileContent]
  implicit lazy val findProjectResponse: OFormat[ProjectResponse] = Json.format[ProjectResponse]
  implicit val updateFileRequestFormat: OFormat[UpdateFileRequest] =
    (JsPath \ "content")
      .format[ProjectFileContent]
      .and((JsPath \ "commit_message").format[String])
      .and((JsPath \ "branch").format[String])(UpdateFileRequest.apply, unlift(UpdateFileRequest.unapply))
  implicit lazy val projectUpdateFileRequestFormat: OFormat[ProjectUpdateFileRequest] =
    ((JsPath \ "project").format[Project] ~ (JsPath \ "projectFile").format[ProjectFile] ~ (JsPath \ "version")
      .formatNullable[PipelineVersion])(
      ProjectUpdateFileRequest.apply,
      unlift(ProjectUpdateFileRequest.unapply)
    )
  implicit lazy val projectBuildConfigurationRequestFormat: OFormat[ProjectBuildConfigurationRequest] =
    Json.format[ProjectBuildConfigurationRequest]
  implicit lazy val projectFileConfigurationFormat: OFormat[ProjectFileConfiguration] = Json.format
  implicit lazy val projectConfigurationFormat: OFormat[ProjectConfiguration] = Json.format
  implicit lazy val pipelineVersionFormat: Format[PipelineVersion] =
    implicitly[Format[String]].inmap(PipelineVersion.apply, _.name)
  implicit lazy val gitlabVersionFormat: OFormat[GitLabVersion] = Json.format[GitLabVersion]
}
