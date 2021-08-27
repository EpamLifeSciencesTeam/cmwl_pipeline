package cromwell.pipeline.controller

import akka.stream.Materializer
import cromwell.pipeline.auth.SecurityDirective
import cromwell.pipeline.service.ServiceModule
import cromwell.pipeline.utils.AuthConfig

import scala.concurrent.ExecutionContext

class ControllerModule(serviceModule: ServiceModule, authConfig: AuthConfig)(
  implicit executionContext: ExecutionContext,
  materializer: Materializer
) {
  lazy val authController: AuthController = new AuthController(serviceModule.authService)
  lazy val userController: UserController = new UserController(serviceModule.userService)
  lazy val securityDirective: SecurityDirective = new SecurityDirective(authConfig)
  lazy val projectController: ProjectController = new ProjectController(serviceModule.projectService)
  lazy val projectFileController: ProjectFileController = new ProjectFileController(serviceModule.projectFileService)
  lazy val configurationController = new ProjectConfigurationController(serviceModule.configurationService)
  lazy val runController: RunController = new RunController(serviceModule.runService)
}
