package cromwell.pipeline.controller

import com.softwaremill.macwire._
import cromwell.pipeline.service.ServiceModule

import scala.concurrent.ExecutionContext

class ControllerModule(serviceModule: ServiceModule)(implicit executionContext: ExecutionContext) {
  lazy val authController: AuthController = wire[AuthController]
  lazy val userManagementController: UserController = wire[UserController]
}
