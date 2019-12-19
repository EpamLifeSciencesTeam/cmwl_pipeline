package cromwell.pipeline.service

import cromwell.pipeline.datastorage.dao.repository.UserRepository
import cromwell.pipeline.datastorage.dto.User.UserEmail
import cromwell.pipeline.datastorage.dto.{ UserId, UserNoCredentials }

import scala.concurrent.{ ExecutionContext, Future }

class UserService(userRepository: UserRepository)(implicit executionContext: ExecutionContext) {
  def deactivateByEmail(email: UserEmail): Future[Option[UserNoCredentials]] =
    for {
      _ <- userRepository.deactivateByEmail(email)
      user <- userRepository.getUserByEmail(email)
    } yield user.map(UserNoCredentials.fromUser)

  def deactivateById(userId: UserId): Future[Option[UserNoCredentials]] =
    for {
      _ <- userRepository.deactivateById(userId)
      user <- userRepository.getUserById(userId)
    } yield user.map(UserNoCredentials.fromUser)
}
