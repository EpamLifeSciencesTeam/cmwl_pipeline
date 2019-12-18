package cromwell.pipeline.service

import cromwell.pipeline.datastorage.dao.repository.UserRepository
import cromwell.pipeline.datastorage.dto.User.UserEmail
import cromwell.pipeline.datastorage.dto.{ UserDeactivationResponse, UserId }

import scala.concurrent.{ ExecutionContext, Future }

class UserService(userRepository: UserRepository)(implicit executionContext: ExecutionContext) {
  def deactivateByEmail(email: UserEmail): Future[Option[UserDeactivationResponse]] =
    for {
      _ <- userRepository.deactivateByEmail(email)
      getUser <- userRepository.getUserByEmail(email)
    } yield getUser.map(UserDeactivationResponse.fromUser)

  def deactivateById(userId: UserId): Future[Option[UserDeactivationResponse]] =
    for {
      _ <- userRepository.deactivateById(userId)
      getUser <- userRepository.getUserById(userId)
    } yield {
      getUser.map(UserDeactivationResponse.fromUser)
    }
}
