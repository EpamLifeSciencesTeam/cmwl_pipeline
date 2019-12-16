package cromwell.pipeline.service

import cromwell.pipeline.datastorage.dao.repository.UserRepository
import cromwell.pipeline.datastorage.dto.User.UserEmail
import cromwell.pipeline.datastorage.dto.{ UserDeactivationByEmailResponse, UserDeactivationByIdResponse, UserId }

import scala.concurrent.{ ExecutionContext, Future }

class UserService(userRepository: UserRepository)(implicit executionContext: ExecutionContext) {
  def deactivateByEmail(email: UserEmail): Future[Option[UserDeactivationByEmailResponse]] =
    userRepository
      .deactivateByEmail(email)
      .flatMap { _ =>
        userRepository.getUserByEmail(email)
      }
      .map { getUser =>
        getUser.map { user =>
          UserDeactivationByEmailResponse(user.email, user.active)
        }
      }

  def deactivateById(userId: UserId): Future[Option[UserDeactivationByIdResponse]] =
    userRepository
      .deactivateById(userId)
      .flatMap { _ =>
        userRepository.getUserById(userId)
      }
      .map { getUser =>
        getUser.map { user =>
          UserDeactivationByIdResponse(user.userId, user.active)
        }
      }

}
