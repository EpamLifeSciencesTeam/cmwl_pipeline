package cromwell.pipeline.datastorage.dao.repository

import cromwell.pipeline.datastorage.dto.PasswordUpdateRequest
import cromwell.pipeline.datastorage.formatters.UserFormatters.updatePasswordRequestFormat
import org.scalatest.{ AsyncWordSpec, Matchers }
import play.api.libs.json._

class FormattersTest extends AsyncWordSpec with Matchers {

  "FormattersTest" when {

    "validate value" should {

      "fails if microtype is unvalid" taggedAs Dao in {

        val unValidUpdatePasswordRequest: JsValue = Json.parse("""
          {
              "currentPassword": "strongPassword1_",
              "newPassword": "222",
              "repeatPassword": "strongPassword1_"
          }
          """)

        unValidUpdatePasswordRequest.validate[PasswordUpdateRequest] shouldBe JsError(
          (
            (JsPath \ "newPassword"),
            JsonValidationError(
              Seq(
                "Microtype is not valid: Chain(Password must be at least 10 characters long, including an uppercase and a lowercase letter, one number and one special character.)"
              )
            )
          )
        )
      }
    }
  }
}
