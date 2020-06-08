package cromwell.pipeline.datastorage.dto

import play.api.libs.functional.syntax._
import play.api.libs.json.{ Format, JsPath }

object File {
  case class UpdateFileRequest(content: String, commitMessage: String, branch: String)

  object UpdateFileRequest {
    implicit val updateFileRequestFormat: Format[UpdateFileRequest] =
      (JsPath \ "branch")
        .format[String]
        .and((JsPath \ "content").format[String])
        .and((JsPath \ "commit_message").format[String])(UpdateFileRequest.apply, unlift(UpdateFileRequest.unapply))
  }
}
