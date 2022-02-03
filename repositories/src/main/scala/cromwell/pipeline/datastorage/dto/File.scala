package cromwell.pipeline.datastorage.dto

import play.api.libs.functional.syntax._
import play.api.libs.json.{ JsPath, Json, OFormat }

object File {
  case class UpdateFileRequest(content: ProjectFileContent, commitMessage: String, branch: String)

  object UpdateFileRequest {
    implicit val updateFileRequestFormat: OFormat[UpdateFileRequest] =
      (JsPath \ "content")
        .format[ProjectFileContent]
        .and((JsPath \ "commit_message").format[String])
        .and((JsPath \ "branch").format[String])(UpdateFileRequest.apply, unlift(UpdateFileRequest.unapply))
  }

  case class DeleteFileRequest(commitMessage: String, branch: String)

  object DeleteFileRequest {
    implicit val deleteFileRequestFormat: OFormat[DeleteFileRequest] = Json.format[DeleteFileRequest]
  }
}
