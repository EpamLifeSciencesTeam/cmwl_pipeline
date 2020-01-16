package cromwell.pipeline.service

import cromwell.pipeline.datastorage.dao.repository.UserRepository
import cromwell.pipeline.datastorage.dto.{ User, UserId, UserNoCredentials }

import scala.concurrent.{ ExecutionContext, Future }
import scala.util.Failure

class UserService(userRepository: UserRepository)(implicit executionContext: ExecutionContext) {

  def getUsersByEmail(emailPattern: String): Future[Seq[User]] = userRepository.getUsersByEmail(emailPattern)

//  def getUsersByEmail(emailPattern: String): Future[Seq[User]] = emailPattern match {
//    case Failure(exc) => Failure(exc)
//    case _            => userRepository.getUsersByEmail(emailPattern)
//  }

  def deactivateUserById(userId: UserId): Future[Option[UserNoCredentials]] =
    for {
      _ <- userRepository.deactivateUserById(userId)
      user <- userRepository.getUserById(userId)
    } yield user.map(UserNoCredentials.fromUser)
}
