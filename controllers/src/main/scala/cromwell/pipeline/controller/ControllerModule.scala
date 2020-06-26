package cromwell.pipeline.controller

import cromwell.pipeline.auth.SecurityDirective
import cromwell.pipeline.service.ServiceModule
import cromwell.pipeline.utils.AuthConfig

import scala.concurrent.ExecutionContext

class ControllerModule(serviceModule: ServiceModule, authConfig: AuthConfig)(
  implicit executionContext: ExecutionContext
) {
  lazy val authController: AuthController = new AuthController(serviceModule.authService)
  lazy val userController: UserController = new UserController(serviceModule.userService)
  lazy val securityDirective: SecurityDirective = new SecurityDirective(authConfig)
  lazy val projectController: ProjectController = new ProjectController(serviceModule.projectService)
  lazy val projectFileController: ProjectFileController = new ProjectFileController(serviceModule.projectFileService)
}
