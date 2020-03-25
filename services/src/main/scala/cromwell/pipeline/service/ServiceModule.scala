package cromwell.pipeline.service

import cromwell.pipeline.datastorage.DatastorageModule
import cromwell.pipeline.utils.GitLabConfig

import scala.concurrent.ExecutionContext

class ServiceModule(datastorageModule: DatastorageModule, config: GitLabConfig)(
  implicit executionContext: ExecutionContext
) {
  lazy val authService: AuthService =
    new AuthService(datastorageModule.userRepository, datastorageModule.authUtils)
  lazy val userService: UserService = new UserService(datastorageModule.userRepository)
  lazy val projectService: ProjectService = new ProjectService(datastorageModule.projectRepository)
  lazy val httpClient: HttpClient = ???
  lazy val projectVersioning: GitLabProjectVersioning = new GitLabProjectVersioning(httpClient, config)

}
