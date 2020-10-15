package cromwell.pipeline.controller

import akka.actor.ActorSystem
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.testkit.{RouteTestTimeout, ScalatestRouteTest}
import akka.testkit.TestDuration
import com.dimafeng.testcontainers.{ForAllTestContainer, PostgreSQLContainer}
import com.typesafe.config.Config
import cromwell.pipeline.ApplicationComponents
import cromwell.pipeline.datastorage.dao.repository.utils.TestUserUtils
import cromwell.pipeline.datastorage.dto.auth.AccessTokenContent
import cromwell.pipeline.utils.TestContainersUtils

import scala.concurrent.duration._
import org.scalatest.{Matchers, WordSpec}

class CromwellControllerItTest extends WordSpec with Matchers with ScalatestRouteTest with ForAllTestContainer {

  override val container: PostgreSQLContainer = TestContainersUtils.getPostgreSQLContainer()
  private implicit lazy val config: Config = TestContainersUtils.getConfigForPgContainer(container)
  private lazy val components: ApplicationComponents = new ApplicationComponents()
  implicit def default(implicit system: ActorSystem) = RouteTestTimeout(10.seconds)
  import components.controllerModule.cromwellController

  private val dummyUser = TestUserUtils.getDummyUser()

    override protected def beforeAll(): Unit = {
      super.beforeAll()
      components.datastorageModule.pipelineDatabaseEngine.updateSchema()
    }

  "CromwellBackendController" when {
    "get engine status" should {
      "return status" in {
        val accessToken = AccessTokenContent(dummyUser.userId)
        Get("/cromwell/engine/status") ~> cromwellController.route(accessToken) ~> check {
          status shouldBe StatusCodes.OK
          responseAs[String] shouldEqual "200"
        }
      }
    }
  }
}
