package cromwell.pipeline.utils.validator

import cromwell.pipeline.datastorage.dto.User.UserEmail

trait DomainValidation {
  def errorCode: String
  def errorMessage: String
  lazy val toMap: Map[String, String] = Seq("errorCode" -> errorCode, "errorMessage" -> errorMessage).toMap
}

object EmailDoesNotMeetCriteria extends DomainValidation {
  val errorCode: String = DomainValidation.incorrectEmailErrorCode
  val errorMessage: String = "Email should match the following pattern <text_1>@<text_2>.<text_3>"
}

object PasswordDoesNotMeetCriteria extends DomainValidation {
  val errorCode: String = DomainValidation.incorrectPasswordErrorCode
  val errorMessage: String = "Password must be at least 10 characters long, " +
    "including an uppercase and a lowercase letter, " +
    "one number and one special character."
}

object FirstNameHasSpecialCharacters extends DomainValidation {
  val errorCode: String = DomainValidation.incorrectFirstNameErrorCode
  val errorMessage: String = "First name cannot contain spaces, numbers or special characters."
}

object LastNameHasSpecialCharacters extends DomainValidation {
  val errorCode: String = DomainValidation.incorrectLastNameErrorCode
  val errorMessage: String = "Last name cannot contain spaces, numbers or special characters."
}

object DomainValidation {
  val incorrectEmailErrorCode = "INCORRECT_EMAIL"
  val incorrectPasswordErrorCode = "INCORRECT_PASSWORD"
  val incorrectFirstNameErrorCode = "INCORRECT_FIRST_NAME"
  val incorrectLastNameErrorCode = "INCORRECT_LAST_NAME"

  val allErrorCodes: Seq[String] =
    Seq(incorrectEmailErrorCode, incorrectPasswordErrorCode, incorrectFirstNameErrorCode, incorrectLastNameErrorCode)

  def checkEmail(userEmail: UserEmail): Boolean = userEmail.matches("^[^@]+@[^\\.]+\\..+$")
  def checkFirstName(fName: String): Boolean = fName.matches("^[a-zA-Z]+$")
  def checkLastName(lName: String): Boolean = lName.matches("^[a-zA-Z]+$")
}
