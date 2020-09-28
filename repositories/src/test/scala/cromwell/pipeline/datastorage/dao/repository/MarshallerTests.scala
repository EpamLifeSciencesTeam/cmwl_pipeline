package cromwell.pipeline.datastorage.dao.repository

import cromwell.pipeline.datastorage.dto._
import cromwell.pipeline.datastorage.dto.auth.{ SignInRequest, SignUpRequest }
import cromwell.pipeline.datastorage.dto.user.{ PasswordUpdateRequest, UserUpdateRequest }
import org.scalatest.{ AsyncWordSpec, Matchers }
import org.scalatestplus.scalacheck.ScalaCheckDrivenPropertyChecks
import play.api.libs.json.{ JsError, JsSuccess }

class MarshallerTests extends AsyncWordSpec with Matchers with ScalaCheckDrivenPropertyChecks {
  import utils.ArbitraryUtils._
  import utils.FormatUtils._

  "MarshallerFormat" when {
    "format UserUpdateRequest" in {
      forAll { (a: UserUpdateRequest) =>
        val parseResult: UserUpdateRequest = userUpdateRequestFormat.reads(userUpdateRequestFormat.writes(a)) match {
          case JsSuccess(value, _) => value
          case JsError(_)          => throw new RuntimeException("Could not parse request")
        }
        parseResult should equal(a)
      }
    }
    "format PasswordUpdateRequest" in {
      forAll { (a: PasswordUpdateRequest) =>
        val parseResult: PasswordUpdateRequest =
          passwordUpdateRequestFormat.reads(passwordUpdateRequestFormat.writes(a)) match {
            case JsSuccess(value, _) => value
            case JsError(_)          => throw new RuntimeException("Could not parse request")
          }
        parseResult should equal(a)
      }
    }
    "format SignUpRequest" in {
      forAll { (a: SignUpRequest) =>
        val parseResult: SignUpRequest = signUpRequestFormat.reads(signUpRequestFormat.writes(a)) match {
          case JsSuccess(value, _) => value
          case JsError(_)          => throw new RuntimeException("Could not parse request")
        }
        parseResult should equal(a)
      }
    }
    "format SignInRequest" in {
      forAll { (a: SignInRequest) =>
        val parseResult: SignInRequest = signInRequestFormat.reads(signInRequestFormat.writes(a)) match {
          case JsSuccess(value, _) => value
          case JsError(_)          => throw new RuntimeException("Could not parse request")
        }
        parseResult should equal(a)
      }
    }
    "format ProjectAdditionalRequest" in {
      forAll { (a: ProjectAdditionRequest) =>
        val parseResult: ProjectAdditionRequest =
          projectAdditionRequestFormat.reads(projectAdditionRequestFormat.writes(a)) match {
            case JsSuccess(value, _) => value
            case JsError(_)          => throw new RuntimeException("Could not parse request")
          }
        parseResult should equal(a)
      }
    }
    "format ProjectDeleteRequest" in {
      forAll { (a: ProjectDeleteRequest) =>
        val parseResult: ProjectDeleteRequest =
          projectDeleteRequestFormat.reads(projectDeleteRequestFormat.writes(a)) match {
            case JsSuccess(value, _) => value
            case JsError(_)          => throw new RuntimeException("Could not parse request")
          }
        parseResult should equal(a)
      }
    }
    "format ProjectUpdateRequest" in {
      forAll { (a: ProjectUpdateRequest) =>
        val parseResult: ProjectUpdateRequest =
          projectUpdateRequestFormat.reads(projectUpdateRequestFormat.writes(a)) match {
            case JsSuccess(value, _) => value
            case JsError(_)          => throw new RuntimeException("Could not parse request")
          }
        parseResult should equal(a)
      }
    }
    "format ProjectFileContent" in {
      forAll { (a: ProjectFileContent) =>
        val parseResult: ProjectFileContent = projectFileContentFormat.reads(projectFileContentFormat.writes(a)) match {
          case JsSuccess(value, _) => value
          case JsError(_)          => throw new RuntimeException("Could not parse request")
        }
        parseResult should equal(a)
      }
    }
    "format ProjectUpdateFileRequest" in {
      forAll { (a: ProjectUpdateFileRequest) =>
        val parseResult: ProjectUpdateFileRequest =
          projectUpdateFileRequestFormat.reads(projectUpdateFileRequestFormat.writes(a)) match {
            case JsSuccess(value, _) => value
            case JsError(_)          => throw new RuntimeException("Could not parse request")
          }
        parseResult should equal(a)
      }
    }
    "format ProjectBuildConfigurationRequest" in {
      forAll { (a: ProjectBuildConfigurationRequest) =>
        val parseResult: ProjectBuildConfigurationRequest =
          projectBuildConfigurationRequestFormat.reads(projectBuildConfigurationRequestFormat.writes(a)) match {
            case JsSuccess(value, _) => value
            case JsError(_)          => throw new RuntimeException("Could not parse request")
          }
        parseResult should equal(a)
      }
    }
  }
}
