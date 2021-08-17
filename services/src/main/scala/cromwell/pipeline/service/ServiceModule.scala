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
  lazy val authService: AuthService = AuthService(userService, authModule.authUtils)
  lazy val userService: UserService = UserService(datastorageModule.userRepository)
  lazy val projectVersioning: GitLabProjectVersioning = new GitLabProjectVersioning(httpClient, config)
  lazy val projectService: ProjectService = ProjectService(datastorageModule.projectRepository, projectVersioning)
  lazy val configurationService: ProjectConfigurationService =
    ProjectConfigurationService(
      datastorageModule.configurationRepository,
      projectService,
      womToolModule.womTool,
      projectVersioning
    )
  lazy val projectFileService: ProjectFileService =
    ProjectFileService(projectService, configurationService, womToolModule.womTool, projectVersioning)
  lazy val runService: RunService = RunService(datastorageModule.runRepository, projectService)
}
