package cromwell.pipeline.datastorage.formatters

import java.nio.file.Paths

import cromwell.pipeline.datastorage.dto._
import cromwell.pipeline.datastorage.formatters.UserFormatters.userIdFormat
import play.api.libs.functional.syntax._
import play.api.libs.json._

object ProjectFormatters {
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
  implicit lazy val versionPlayFormat: OFormat[Version] = Json.format[Version]
  implicit object ProjectFileFormat extends Format[ProjectFile] {
    override def reads(json: JsValue): JsResult[ProjectFile] =
      JsSuccess(ProjectFile(Paths.get((json \ "path").as[String]), (json \ "content").as[String]))

    override def writes(o: ProjectFile): JsValue = JsObject(
      Seq("path" -> JsString(o.path.toString), "content" -> JsString(o.content))
    )
  }
  implicit lazy val visibilityFormat: Format[Visibility] =
    implicitly[Format[String]].inmap(Visibility.fromString, Visibility.toString)
  implicit lazy val projectIdFormat: Format[ProjectId] = implicitly[Format[String]].inmap(ProjectId.apply, _.value)
  implicit lazy val repositoryFormat: Format[Repository] = implicitly[Format[String]].inmap(Repository.apply, _.value)
  implicit lazy val projectAdditionFormat: OFormat[ProjectAdditionRequest] = Json.format[ProjectAdditionRequest]
  implicit lazy val projectDeleteFormat: OFormat[ProjectDeleteRequest] = Json.format[ProjectDeleteRequest]
  implicit lazy val updateRequestFormat: OFormat[ProjectUpdateRequest] = Json.format[ProjectUpdateRequest]
  implicit lazy val validateFileRequestFormat: OFormat[FileContent] = Json.format[FileContent]
  implicit lazy val findProjectResponse: OFormat[ProjectResponse] = Json.format[ProjectResponse]
  implicit lazy val updateFileRequestFormat: Format[UpdateFileRequest] =
    (JsPath \ "branch")
      .format[String]
      .and((JsPath \ "content").format[String])
      .and((JsPath \ "commit_message").format[String])(UpdateFileRequest.apply, unlift(UpdateFileRequest.unapply))
  implicit lazy val projectUpdateFileRequestFormat: OFormat[ProjectUpdateFileRequest] =
    ((JsPath \ "project").format[Project] ~ (JsPath \ "projectFile").format[ProjectFile] ~ (JsPath \ "version")
      .formatNullable[Version])(
      ProjectUpdateFileRequest.apply,
      unlift(ProjectUpdateFileRequest.unapply)
    )
}
