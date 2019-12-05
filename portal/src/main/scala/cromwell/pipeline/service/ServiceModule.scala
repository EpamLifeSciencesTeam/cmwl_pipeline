package cromwell.pipeline.service

import com.softwaremill.macwire._
import cromwell.pipeline.datastorage.DatastorageModule
import cromwell.pipeline.utils.UtilsModule

import scala.concurrent.ExecutionContext

@Module
class ServiceModule(datastorageModule: DatastorageModule, utilsModule: UtilsModule)(
  implicit executionContext: ExecutionContext
) {
  lazy val authService: AuthService = wire[AuthService]
  lazy val userService: UserService = wire[UserService]
}
