package cromwell.pipeline.service

import cromwell.pipeline.datastorage.dao.repository.UserRepository
import cromwell.pipeline.datastorage.dto.user.DeactivateUserRequestByEmail
import cromwell.pipeline.datastorage.dto.{ UserDeactivationByEmailResponse, UserDeactivationByIdResponse, UserId }
import cromwell.pipeline.utils.auth.AuthUtils

import scala.concurrent.{ ExecutionContext, Future }

class UserService(userRepository: UserRepository, authUtils: AuthUtils)(implicit executionContext: ExecutionContext) {
  def deactivateByEmail(request: DeactivateUserRequestByEmail): Future[Option[UserDeactivationByEmailResponse]] =
    userRepository
      .deactivateByEmail(request.email)
      .flatMap { _ =>
        userRepository.getUserByEmail(request.email)
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
