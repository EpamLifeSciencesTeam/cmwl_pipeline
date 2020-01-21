package cromwell.pipeline.service

import cromwell.pipeline.datastorage.DatastorageModule

import scala.concurrent.ExecutionContext

class ServiceModule(datastorageModule: DatastorageModule)(
  implicit executionContext: ExecutionContext
) {
  lazy val authService: AuthService =
    new AuthService(datastorageModule.userRepository, datastorageModule.authUtils)
  lazy val userService: UserService = new UserService(datastorageModule.userRepository)
  lazy val projectService: ProjectService = new ProjectService(datastorageModule.projectRepository)
}
