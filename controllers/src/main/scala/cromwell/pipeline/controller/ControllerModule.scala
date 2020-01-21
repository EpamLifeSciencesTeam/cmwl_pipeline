package cromwell.pipeline.controller

import cromwell.pipeline.service.ServiceModule

import scala.concurrent.ExecutionContext

class ControllerModule(serviceModule: ServiceModule)(implicit executionContext: ExecutionContext) {
  lazy val authController: AuthController = new AuthController(serviceModule.authService)
  lazy val userController: UserController = new UserController(serviceModule.userService)
}
