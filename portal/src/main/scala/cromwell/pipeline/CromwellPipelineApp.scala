package cromwell.pipeline

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.{ RejectionHandler, Route, ValidationRejection }
import cromwell.pipeline.auth.token.MissingAccessTokenRejection
import cromwell.pipeline.datastorage.dto.auth.AccessTokenContent
import cromwell.pipeline.utils.configs.ConfigJsonOps
import org.slf4j.LoggerFactory

import scala.concurrent.ExecutionContextExecutor

object CromwellPipelineApp extends App {

  implicit val system: ActorSystem = ActorSystem("cromwell-pipeline")
  implicit val executionContext: ExecutionContextExecutor = system.dispatcher

  val log = LoggerFactory.getLogger(CromwellPipelineApp.getClass)
  val components = new ApplicationComponents()

  import components.applicationConfig
  import components.applicationConfig.webServiceConfig
  import components.controllerModule._
  import components.datastorageModule._

  pipelineDatabaseEngine.updateSchema()

  type SecuredRoute = AccessTokenContent => Route
  def routeCombiner(routes: SecuredRoute*): SecuredRoute = token => concat(routes.map(_(token)): _*)

  implicit val rejectionHandler: RejectionHandler = RejectionHandler
    .newBuilder()
    .handle {
      case ValidationRejection(msg, _)      => complete(StatusCodes.BadRequest, msg)
      case MissingAccessTokenRejection(msg) => complete(StatusCodes.Unauthorized, msg)
    }
    .result()
    .withFallback(RejectionHandler.default)

  val route = authController.route ~ securityDirective.authenticated {
    routeCombiner(
      userController.route,
      projectController.route,
      projectFileController.route,
      runController.route,
      configurationController.route
    )
  }

  log.info(s"Server online at http://${webServiceConfig.interface}:${webServiceConfig.port}/")
  log.info(ConfigJsonOps.configToJsonString(applicationConfig))
  Http().newServerAt(webServiceConfig.interface, webServiceConfig.port).bindFlow(route)
}
