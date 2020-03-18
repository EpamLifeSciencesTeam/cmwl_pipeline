package cromwell.pipeline

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import akka.stream.ActorMaterializer
import cromwell.pipeline.datastorage.utils.auth.AccessTokenContent
import org.slf4j.LoggerFactory

import scala.concurrent.ExecutionContextExecutor

object CromwellPipelineApp extends App {

  implicit val system: ActorSystem = ActorSystem("cromwell-pipeline")
  implicit val materializer: ActorMaterializer = ActorMaterializer()
  implicit val executionContext: ExecutionContextExecutor = system.dispatcher

  val log = LoggerFactory.getLogger(CromwellPipelineApp.getClass)
  val components = new ApplicationComponents()

  import components.applicationConfig.webServiceConfig
  import components.controllerModule._
  import components.datastorageModule._

  pipelineDatabaseEngine.updateSchema()

  type SecuredRoute = AccessTokenContent => Route
  def routeCombiner(routes: SecuredRoute*): SecuredRoute = token => concat(routes.map(_(token)): _*)

  val route = authController.route ~ securityDirective.authenticated {
    routeCombiner(userController.route, projectController.route)
  }

  log.info(s"Server online at http://${webServiceConfig.interface}:${webServiceConfig.port}/")
  Http().bindAndHandle(route, webServiceConfig.interface, webServiceConfig.port)
}
