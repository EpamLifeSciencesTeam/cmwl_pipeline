package cromwell.pipeline.datastorage.utils.validator

import cats.data.Validated._
import cats.data._
import cats.implicits._
import cromwell.pipeline.datastorage.dto.PasswordUpdateRequest
import cromwell.pipeline.datastorage.dto.auth.SignUpRequest

object FormValidatorNel {

  type ValidationResult[A] = ValidatedNel[DomainValidation, A]

  private def validateEmail(email: String): ValidationResult[String] =
    if (email.matches("^[^@]+@[^\\.]+\\..+$")) email.validNel else EmailDoesNotMeetCriteria.invalidNel

  private def validatePassword(password: String): ValidationResult[String] =
    if (password.matches("(?=^.{10,}$)((?=.*\\d)|(?=.*\\W+))(?![.\\n])(?=.*[A-Z])(?=.*[a-z]).*$")) password.validNel
    else PasswordDoesNotMeetCriteria.invalidNel

  private def validateFirstName(firstName: String): ValidationResult[String] =
    if (firstName.matches("^[a-zA-Z]+$")) firstName.validNel else FirstNameHasSpecialCharacters.invalidNel

  private def validateLastName(lastName: String): ValidationResult[String] =
    if (lastName.matches("^[a-zA-Z]+$")) lastName.validNel else LastNameHasSpecialCharacters.invalidNel

  def validateForm(signUpRequest: SignUpRequest): ValidationResult[SignUpRequest] =
    (
      validateEmail(signUpRequest.email),
      validatePassword(signUpRequest.password),
      validateFirstName(signUpRequest.firstName),
      validateLastName(signUpRequest.lastName)
    ).mapN(SignUpRequest.apply)

  def validateForm(passwordUpdateRequest: PasswordUpdateRequest): ValidationResult[String] =
    validatePassword(passwordUpdateRequest.newPassword)
}
