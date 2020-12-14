package cromwell.pipeline

import akka.actor.ActorSystem
import akka.stream.Materializer
import com.typesafe.config.{ Config, ConfigFactory }
import cromwell.pipeline.auth.AuthModule
import cromwell.pipeline.controller.{ AkkaHttpClient, ControllerModule }
import cromwell.pipeline.datastorage.DatastorageModule
import cromwell.pipeline.service.{ HttpClient, ServiceModule, WomToolModule }
import cromwell.pipeline.utils.ApplicationConfig

import scala.concurrent.ExecutionContext

final class ApplicationComponents(
  implicit val config: Config = ConfigFactory.load(),
  val executionContext: ExecutionContext,
  val actorSystem: ActorSystem
) {
  lazy val applicationConfig: ApplicationConfig = ApplicationConfig.load(config)
  lazy val authModule: AuthModule = new AuthModule(applicationConfig.authConfig)
  lazy val datastorageModule: DatastorageModule = new DatastorageModule(applicationConfig)
  lazy val httpClient: HttpClient = new AkkaHttpClient()
  lazy val womToolModule: WomToolModule = new WomToolModule()
  lazy val serviceModule: ServiceModule =
    new ServiceModule(datastorageModule, authModule, httpClient, applicationConfig.gitLabConfig, womToolModule)
  lazy val controllerModule: ControllerModule = new ControllerModule(serviceModule, applicationConfig.authConfig)

}
