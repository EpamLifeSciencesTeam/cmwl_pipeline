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

  def initializeApplicationComponents: Try[ApplicationComponents] = Try {
    new ApplicationComponents()
  }

  def updateDb(components: ApplicationComponents): Try[Unit] = {
    import components.datastorageModule._
    pipelineDatabaseEngine.updateSchema()
  }

  def startHttpServer(components: ApplicationComponents): Try[Unit] =
    Try {
      import components.applicationConfig
      import components.applicationConfig.webServiceConfig
      val route = new CromwellPipelineRoute(applicationConfig, components.controllerModule).route
      log.info(s"Server online at http://${webServiceConfig.interface}:${webServiceConfig.port}/")
      log.info(ConfigJsonOps.configToJsonString(applicationConfig))
      Http().newServerAt(webServiceConfig.interface, webServiceConfig.port).bindFlow(route)
    }

  val app = for {

    applicationComponents <- initializeApplicationComponents

    _ <- updateDb(applicationComponents)

    _ <- startHttpServer(applicationComponents)

  } yield ()

  app match {
    case Success(_) => log.info(s"Application is running!")

    case Failure(exception) =>
      log.error(s"Application cannot be started:", exception)
      system.terminate()
  }
}
