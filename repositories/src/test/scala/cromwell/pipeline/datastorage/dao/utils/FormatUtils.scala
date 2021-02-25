package cromwell.pipeline.datastorage.dao.utils

import cromwell.pipeline.datastorage.dto._
import cromwell.pipeline.datastorage.dto.auth.{ SignInRequest, SignUpRequest }
import cromwell.pipeline.datastorage.dto.user.{ PasswordUpdateRequest, UserUpdateRequest }
import play.api.libs.json.Format

object FormatUtils {
  implicit val userUpdateRequestFormat: Format[UserUpdateRequest] = UserUpdateRequest.updateRequestFormat
  implicit val passwordUpdateRequestFormat: Format[PasswordUpdateRequest] =
    PasswordUpdateRequest.updatePasswordRequestFormat
  implicit val signUpRequestFormat: Format[SignUpRequest] = SignUpRequest.signUpRequestFormat
  implicit val signInRequestFormat: Format[SignInRequest] = SignInRequest.signInRequestFormat
  implicit val projectAdditionRequestFormat: Format[ProjectAdditionRequest] =
    ProjectAdditionRequest.projectAdditionFormat
  implicit val projectDeleteRequestFormat: Format[ProjectDeleteRequest] = ProjectDeleteRequest.projectDeleteFormat
  implicit val projectUpdateNameRequestFormat: Format[ProjectUpdateNameRequest] =
    ProjectUpdateNameRequest.updateRequestFormat
  implicit val projectFileContentFormat: Format[ProjectFileContent] = ProjectFileContent.projectFileContentFormat
  implicit val gitLabFileContentFormat: Format[GitLabFileContent] = GitLabFileContent.encodingProjectFileContentFormat
  implicit val projectUpdateFileRequestFormat: Format[ProjectUpdateFileRequest] =
    ProjectUpdateFileRequest.projectUpdateFileRequestFormat
  implicit val projectBuildConfigurationRequestFormat: Format[ProjectBuildConfigurationRequest] =
    ProjectBuildConfigurationRequest.projectBuildConfigurationRequestFormat
}
