package cromwell.pipeline.service

import cromwell.pipeline.datastorage.dao.repository.UserRepository
import cromwell.pipeline.datastorage.dto.{ User, UserId, UserNoCredentials }
import scala.concurrent.{ ExecutionContext, Future }

class UserService(userRepository: UserRepository)(implicit executionContext: ExecutionContext) {

  def getUsersByEmail(emailPattern: String): Future[Seq[User]] =
    userRepository.getUsersByEmail(emailPattern)

  def deactivateUserById(userId: UserId): Future[Option[UserNoCredentials]] =
    for {
      _ <- userRepository.deactivateUserById(userId)
      user <- userRepository.getUserById(userId)
    } yield user.map(UserNoCredentials.fromUser)
}
