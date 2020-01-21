package cromwell.pipeline

import com.typesafe.config.{ Config, ConfigFactory }
import cromwell.pipeline.controller.ControllerModule
import cromwell.pipeline.datastorage.DatastorageModule
import cromwell.pipeline.service.ServiceModule
import cromwell.pipeline.utils.ApplicationConfig

import scala.concurrent.ExecutionContext

final class ApplicationComponents(
  implicit val config: Config = ConfigFactory.load(),
  val executionContext: ExecutionContext
) {
  lazy val applicationConfig: ApplicationConfig = ApplicationConfig.load(config)
  lazy val datastorageModule: DatastorageModule = new DatastorageModule(applicationConfig)
  lazy val serviceModule: ServiceModule = new ServiceModule(datastorageModule)
  lazy val controllerModule: ControllerModule = new ControllerModule(serviceModule)
}
