package cromwell.pipeline.datastorage.dao.repository.utils

import cromwell.pipeline.datastorage.dto._
import cromwell.pipeline.datastorage.dto.auth.{ SignInRequest, SignUpRequest }
import cromwell.pipeline.datastorage.dto.user.{ PasswordUpdateRequest, UserUpdateRequest }
import play.api.libs.json.{ Format, Json }

object FormatUtils {
  implicit val userUpdateRequestFormat: Format[UserUpdateRequest] = Json.format[UserUpdateRequest]
  implicit val passwordUpdateRequestFormat: Format[PasswordUpdateRequest] = Json.format[PasswordUpdateRequest]
  implicit val signUpRequestFormat: Format[SignUpRequest] = Json.format[SignUpRequest]
  implicit val signInRequestFormat: Format[SignInRequest] = Json.format[SignInRequest]
  implicit val projectAdditionRequestFormat: Format[ProjectAdditionRequest] = Json.format[ProjectAdditionRequest]
  implicit val projectDeleteRequestFormat: Format[ProjectDeleteRequest] = Json.format[ProjectDeleteRequest]
  implicit val projectUpdateRequestFormat: Format[ProjectUpdateRequest] = Json.format[ProjectUpdateRequest]
  implicit val projectFileContentFormat: Format[ProjectFileContent] = Json.format[ProjectFileContent]
  implicit val projectUpdateFileRequestFormat: Format[ProjectUpdateFileRequest] = Json.format[ProjectUpdateFileRequest]
  implicit val projectBuildConfigurationRequestFormat: Format[ProjectBuildConfigurationRequest] =
    Json.format[ProjectBuildConfigurationRequest]
}
