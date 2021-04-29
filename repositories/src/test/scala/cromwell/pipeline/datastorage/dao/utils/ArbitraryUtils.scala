package cromwell.pipeline.datastorage.dao.utils

import cromwell.pipeline.datastorage.dto._
import cromwell.pipeline.datastorage.dto.auth.{ SignInRequest, SignUpRequest }
import cromwell.pipeline.datastorage.dto.user.{ PasswordUpdateRequest, UserUpdateRequest }
import org.scalacheck.Arbitrary

object ArbitraryUtils {
  import GeneratorUtils._

  implicit val arbitraryUserUpdateRequest: Arbitrary[UserUpdateRequest] = Arbitrary(userUpdateRequestGen)
  implicit val arbitraryPasswordUpdateRequest: Arbitrary[PasswordUpdateRequest] = Arbitrary(passwordUpdateRequestGen)
  implicit val arbitrarySignUpRequest: Arbitrary[SignUpRequest] = Arbitrary(signUpRequestGen)
  implicit val arbitrarySignInRequest: Arbitrary[SignInRequest] = Arbitrary(signInRequestGen)
  implicit val arbitraryProjectAdditionRequest: Arbitrary[ProjectAdditionRequest] = Arbitrary(projectAdditionRequestGen)
  implicit val arbitraryProjectDeleteRequest: Arbitrary[ProjectDeleteRequest] = Arbitrary(projectDeleteRequestGen)
  implicit val arbitraryProjectUpdateNameRequest: Arbitrary[ProjectUpdateNameRequest] =
    Arbitrary(projectUpdateNameRequestGen)
  implicit val arbitraryProjectFileContent: Arbitrary[ProjectFileContent] = Arbitrary(projectFileContentGen)
  implicit val arbitraryGitLabFileContent: Arbitrary[GitLabFileContent] = Arbitrary(gitLabFileContentGen)
  implicit val arbitraryProjectUpdateFileRequest: Arbitrary[ProjectUpdateFileRequest] =
    Arbitrary(projectUpdateFileRequestGen)
  implicit val arbitraryProjectConfiguration: Arbitrary[ProjectConfiguration] = Arbitrary(projectConfigurationGen)
}
