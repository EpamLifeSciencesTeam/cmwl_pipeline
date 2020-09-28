package cromwell.pipeline.datastorage.dao.repository.utils

import cromwell.pipeline.datastorage.dto._
import cromwell.pipeline.datastorage.dto.auth.{ SignInRequest, SignUpRequest }
import cromwell.pipeline.datastorage.dto.user.{ PasswordUpdateRequest, UserUpdateRequest }
import org.scalacheck.Arbitrary

object ArbitraryUtils {
  import GeneratorUtils._

  implicit def arbitraryUserUpdateRequest: Arbitrary[UserUpdateRequest] = Arbitrary(userUpdateRequestGen)
  implicit def arbitraryPasswordUpdateRequest: Arbitrary[PasswordUpdateRequest] = Arbitrary(passwordUpdateRequestGen)
  implicit def arbitrarySignUpRequest: Arbitrary[SignUpRequest] = Arbitrary(signUpRequestGen)
  implicit def arbitrarySignInRequest: Arbitrary[SignInRequest] = Arbitrary(signInRequestGen)
  implicit def arbitraryProjectAdditionRequest: Arbitrary[ProjectAdditionRequest] = Arbitrary(projectAdditionRequestGen)
  implicit def arbitraryProjectDeleteRequest: Arbitrary[ProjectDeleteRequest] = Arbitrary(projectDeleteRequestGen)
  implicit def arbitraryProjectUpdateRequest: Arbitrary[ProjectUpdateRequest] = Arbitrary(projectUpdateRequestGen)
  implicit def arbitraryProjectFileContent: Arbitrary[ProjectFileContent] = Arbitrary(projectFileContentGen)
  implicit def arbitraryProjectUpdateFileRequest: Arbitrary[ProjectUpdateFileRequest] = Arbitrary(
    projectUpdateFileRequestGen
  )
  implicit def arbitraryProjectBuildConfigurationRequest: Arbitrary[ProjectBuildConfigurationRequest] = Arbitrary(
    projectBuildConfigurationRequestGen
  )
}
