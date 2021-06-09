package cromwell.pipeline

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.{ RejectionHandler, ValidationRejection }
import cromwell.pipeline.auth.token.MissingAccessTokenRejection
import cromwell.pipeline.utils.configs.ConfigJsonOps
import org.slf4j.LoggerFactory

import scala.concurrent.ExecutionContextExecutor
import scala.util.{ Failure, Success, Try }

object CromwellPipelineApp extends App {

  val log = LoggerFactory.getLogger(CromwellPipelineApp.getClass)

  implicit val system: ActorSystem = ActorSystem("cromwell-pipeline")
  implicit val executionContext: ExecutionContextExecutor = system.dispatcher
  implicit val rejectionHandler: RejectionHandler = RejectionHandler
    .newBuilder()
    .handle {
      case ValidationRejection(msg, _)      => complete(StatusCodes.BadRequest, msg)
      case MissingAccessTokenRejection(msg) => complete(StatusCodes.Unauthorized, msg)
    }
    .result()
    .withFallback(RejectionHandler.default)

  Try {
    val components = new ApplicationComponents()

    import components.applicationConfig
    import components.applicationConfig.webServiceConfig
    import components.datastorageModule._

    pipelineDatabaseEngine.updateSchema()

    val route = new CromwellPipelineRoute(components.applicationConfig, components.controllerModule).route

    log.info(s"Server online at http://${webServiceConfig.interface}:${webServiceConfig.port}/")
    log.info(ConfigJsonOps.configToJsonString(applicationConfig))
    Http().newServerAt(webServiceConfig.interface, webServiceConfig.port).bindFlow(route)
  } match {
    case Success(_) =>
      log.info(s"Application is running!")
    case Failure(e) =>
      system.terminate()
      log.error(s"Application cannot be started:", e)
  }
}
