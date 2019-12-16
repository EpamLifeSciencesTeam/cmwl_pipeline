package cromwell.pipeline.service

import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Directives.complete
import akka.http.scaladsl.server.Route
import cromwell.pipeline.datastorage.dao.repository.UserRepository
import cromwell.pipeline.datastorage.dto.user.{ PasswordUpdateRequest, UserUpdateRequest }
import cromwell.pipeline.datastorage.dto.{ User, UserId }
import cromwell.pipeline.utils.StringUtils
import play.api.libs.json.JsError
import play.api.libs.json.JsResult.Exception

import scala.concurrent.{ ExecutionContext, Future }
import scala.util.Random

class UserService(userRepository: UserRepository)(implicit executionContext: ExecutionContext) {

  def updateUser(id: String, request: UserUpdateRequest): Future[Int] = {
    val updatedUser =
      User(UserId(id), email = request.email, firstName = request.firstName, lastName = request.lastName)
    userRepository.updateUser(updatedUser)
  }

  def updateUserPassword(id: String, request: PasswordUpdateRequest): Future[Int] =
    if (request.newPassword != request.repeatPassword) {
      Future.failed(Exception(JsError("Password doesn't match")))
    } else {
      val passwordSalt = getSalt
      val passwordHash = getPasswordHash(request.currentPassword, passwordSalt)
      val updatedUser = User(userId = UserId(id), passwordSalt = passwordSalt, passwordHash = passwordHash)
      userRepository.updateUserPassword(updatedUser)
    }

  def getStatus(count: Int): Route =
    if (count == 1) complete(StatusCodes.NoContent)
    else complete(StatusCodes.NotAcceptable, "Requests id doesn't exist")

  private def getSalt: String = Random.nextLong().toHexString
  private def getPasswordHash(password: String, salt: String): String =
    StringUtils.calculatePasswordHash(password, salt)
}
