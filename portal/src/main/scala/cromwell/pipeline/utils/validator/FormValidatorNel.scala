package cromwell.pipeline.utils.validator

import cats.data.Validated._
import cats.data._
import cats.implicits._
import cromwell.pipeline.datastorage.dto.auth.SignUpRequest

object FormValidatorNel {

  type ValidationResult[A] = ValidatedNel[DomainValidation, A]

  private def validateEmail(email: String): ValidationResult[String] =
    if (DomainValidation.checkEmail(email)) email.validNel else EmailDoesNotMeetCriteria.invalidNel

  private def validatePassword(password: String): ValidationResult[String] =
    if (password.matches("(?=^.{10,}$)((?=.*\\d)|(?=.*\\W+))(?![.\\n])(?=.*[A-Z])(?=.*[a-z]).*$"))
      password.validNel
    else PasswordDoesNotMeetCriteria.invalidNel

  private def validateFirstName(firstName: String): ValidationResult[String] =
    if (DomainValidation.checkFirstName(firstName)) firstName.validNel else FirstNameHasSpecialCharacters.invalidNel

  private def validateLastName(lastName: String): ValidationResult[String] =
    if (DomainValidation.checkLastName(lastName)) lastName.validNel else LastNameHasSpecialCharacters.invalidNel

  def validateForm(signUpRequest: SignUpRequest): ValidationResult[SignUpRequest] =
    (
      validateEmail(signUpRequest.email),
      validatePassword(signUpRequest.password),
      validateFirstName(signUpRequest.firstName),
      validateLastName(signUpRequest.lastName)
    ).mapN(SignUpRequest.apply)

}
