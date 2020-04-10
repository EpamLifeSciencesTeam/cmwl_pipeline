package cromwell.pipeline

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import com.typesafe.config.{ Config, ConfigFactory }
import cromwell.pipeline.controller.{ AkkaHttpClient, ControllerModule }
import cromwell.pipeline.datastorage.DatastorageModule
import cromwell.pipeline.service.{ HttpClient, ServiceModule }
import cromwell.pipeline.utils.ApplicationConfig

import scala.concurrent.ExecutionContext

final class ApplicationComponents(
  implicit val config: Config = ConfigFactory.load(),
  val executionContext: ExecutionContext,
  val actorSystem: ActorSystem,
  val materializer: ActorMaterializer
) {
  lazy val applicationConfig: ApplicationConfig = ApplicationConfig.load(config)
  lazy val datastorageModule: DatastorageModule = new DatastorageModule(applicationConfig)
  lazy val httpClient: HttpClient = new AkkaHttpClient()
  lazy val serviceModule: ServiceModule =
    new ServiceModule(datastorageModule, httpClient, applicationConfig.gitLabConfig)
  lazy val controllerModule: ControllerModule = new ControllerModule(serviceModule)

}
