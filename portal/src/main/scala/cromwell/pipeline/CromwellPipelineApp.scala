package cromwell.pipeline

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import akka.stream.ActorMaterializer
import com.softwaremill.macwire._
import cromwell.pipeline.utils.auth.AccessTokenContent
import org.slf4j.LoggerFactory

//import wdltool.graph.GraphPrint
//import womtool.cmdline.HighlightCommandLine

import scala.concurrent.ExecutionContextExecutor

object CromwellPipelineApp extends App {

  implicit val system: ActorSystem = ActorSystem("cromwell-pipeline")
  implicit val materializer: ActorMaterializer = ActorMaterializer()
  implicit val executionContext: ExecutionContextExecutor = system.dispatcher

  val log = LoggerFactory.getLogger(CromwellPipelineApp.getClass)
  val components = wire[ApplicationComponents]

  import components.applicationConfig.webServiceConfig
  import components.controllerModule._
  import components.datastorageModule._
  import components.utilsModule._

  val bla = (left: AccessTokenContent => Route, right: AccessTokenContent => Route) =>
    (token: AccessTokenContent) => concat(left(token), right(token))

  pipelineDatabaseEngine.updateSchema()

//  val route = authController.route ~ securityDirective.authenticated { userController.route }
  val route = authController.route ~ securityDirective.authenticated { bla(userController.route, userController.route) }

  log.info(s"Server online at http://${webServiceConfig.interface}:${webServiceConfig.port}/")
  Http().bindAndHandle(route, webServiceConfig.interface, webServiceConfig.port)
}
