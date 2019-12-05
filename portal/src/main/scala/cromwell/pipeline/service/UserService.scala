package cromwell.pipeline.service

import cromwell.pipeline.datastorage.dao.repository.UserRepository
import cromwell.pipeline.datastorage.dto.user.UpdateRequest
import cromwell.pipeline.datastorage.dto.{User, UserId}
import cromwell.pipeline.utils.StringUtils

import scala.concurrent.{ExecutionContext, Future}
import scala.util.Random

class UserService(userRepository: UserRepository)(implicit executionContext: ExecutionContext) {

  def update(id: String, request: UpdateRequest): Future[Int] = {
    val updatedUser = User(
      userId = UserId(id),
      email = request.email,
      passwordSalt = Random.nextLong().toHexString,
      passwordHash = StringUtils.calculatePasswordHash(request.password, Random.nextLong().toHexString),
      firstName = request.firstName,
      lastName = request.lastName
    )
    userRepository.updateUser(updatedUser)
  }
}

