package cromwell.pipeline.service

import cromwell.pipeline.datastorage.dao.repository.UserRepository
import cromwell.pipeline.datastorage.dto.UserId
import cromwell.pipeline.datastorage.dto.management.DeactivateUserRequestByEmail
import cromwell.pipeline.utils.auth.AuthUtils

import scala.concurrent.{ExecutionContext, Future}

class UserManagementService (userRepository: UserRepository, authUtils: AuthUtils)(implicit executionContext: ExecutionContext){
  def deactivateByEmail(request: DeactivateUserRequestByEmail): Future[Int] = {
    userRepository.deactivateUserByEmail(request.email)
  }

  def deactivateById(userId: UserId): Future[Int] = {
    userRepository.deactivateUserById(userId)
  }

}
