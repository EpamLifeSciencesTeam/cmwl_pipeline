package cromwell.pipeline.service

import cromwell.pipeline.datastorage.dao.repository.UserRepository
import cromwell.pipeline.datastorage.dto.management.DeactivateUserRequest
import cromwell.pipeline.utils.auth.AuthUtils

import scala.concurrent.{ExecutionContext, Future}

class UserManagementService (userRepository: UserRepository, authUtils: AuthUtils)(implicit executionContext: ExecutionContext){
  def deactivate(request: DeactivateUserRequest): Future[Int] = {
    userRepository.deactivateUser(request.email)
  }

}
