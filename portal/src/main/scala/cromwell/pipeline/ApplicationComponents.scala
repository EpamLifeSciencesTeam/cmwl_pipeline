package cromwell.pipeline

import com.softwaremill.macwire._
import com.typesafe.config.{ Config, ConfigFactory }
import cromwell.pipeline.controller.ControllerModule
import cromwell.pipeline.datastorage.DatastorageModule
import cromwell.pipeline.service.ServiceModule
import cromwell.pipeline.utils.UtilsModule

import scala.concurrent.ExecutionContext

final class ApplicationComponents(
  implicit val config: Config = ConfigFactory.load(),
  val executionContext: ExecutionContext
) {
  lazy val applicationConfig: ApplicationConfig = wireWith(ApplicationConfig.load _)
  lazy val utilsModule: UtilsModule = wire[UtilsModule]
  lazy val datastorageModule: DatastorageModule = wire[DatastorageModule]
  lazy val serviceModule: ServiceModule = wire[ServiceModule]
  lazy val controllerModule: ControllerModule = wire[ControllerModule]
}
