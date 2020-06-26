package cromwell.pipeline.service

import cromwell.pipeline.auth.AuthModule
import cromwell.pipeline.datastorage.DatastorageModule
import cromwell.pipeline.utils.GitLabConfig

import scala.concurrent.ExecutionContext

class ServiceModule(
  datastorageModule: DatastorageModule,
  authModule: AuthModule,
  httpClient: HttpClient,
  config: GitLabConfig,
  womToolModule: WomToolModule
)(
  implicit executionContext: ExecutionContext
) {
  lazy val authService: AuthService =
    new AuthService(datastorageModule.userRepository, authModule.authUtils)
  lazy val userService: UserService = new UserService(datastorageModule.userRepository)
  lazy val projectVersioning: GitLabProjectVersioning = new GitLabProjectVersioning(httpClient, config)
  lazy val projectService: ProjectService = new ProjectService(datastorageModule.projectRepository, projectVersioning)

  lazy val projectFileService: ProjectFileService = new ProjectFileService(womToolModule.womTool, projectVersioning)
}
