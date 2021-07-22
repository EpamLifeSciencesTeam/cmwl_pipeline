package cromwell.pipeline.controller

import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.testkit.ScalatestRouteTest
import com.dimafeng.testcontainers.{ForAllTestContainer, MongoDBContainer}
import com.typesafe.config.Config
import cromwell.pipeline.ApplicationComponents
import cromwell.pipeline.datastorage.dao.utils.{TestProjectUtils, TestUserUtils}
import cromwell.pipeline.datastorage.dto.auth.AccessTokenContent
import cromwell.pipeline.datastorage.dto._
import cromwell.pipeline.utils.{TestContainersUtils, TestTimeout}
import de.heikoseeberger.akkahttpplayjson.PlayJsonSupport
import org.scalatest.{AsyncWordSpec, Matchers}

import java.nio.file.Path
import scala.concurrent.Await


class ProjectConfigurationControllerItTest
    extends AsyncWordSpec
    with Matchers
    with ScalatestRouteTest
    with PlayJsonSupport
    with ForAllTestContainer
    with TestTimeout {

  override val container: MongoDBContainer = TestContainersUtils.getMongoContainer()
  implicit lazy val config: Config = TestContainersUtils.getConfigForMongoContainer(container)
  private lazy val components: ApplicationComponents = new ApplicationComponents()

  import components.controllerModule.configurationController
  import components.datastorageModule.configurationRepository

  override protected def beforeAll(): Unit = {
    super.beforeAll()
    Await.result(
      configurationRepository.addConfiguration(projectConfiguration),
      timeoutAsDuration
    )
  }

  private val dummyUser = TestUserUtils.getDummyUser()

  private val dummyProject = TestProjectUtils.getDummyProject(ownerId = dummyUser.userId)

  private val projectConfigurationId = ProjectConfigurationId.randomId

  private val projectConfiguration = new ProjectConfiguration(
    projectConfigurationId,
    dummyProject.projectId,
    true,
    List[ProjectFileConfiguration](
      new ProjectFileConfiguration(Path.of("hello.wdl"),
      List(new FileParameter("hello", StringTyped(Some("Hello")))))),
    ProjectConfigurationVersion("v1")
  )

  "ProjectConfigurationController" when {
    "getConfigurationById" should {
      "return a current configuration" in {
        val accessToken = AccessTokenContent(dummyProject.ownerId)

        Get("/configurations?project_id=" + projectConfigurationId) ~> configurationController.route(accessToken) ~> check {
          status shouldBe StatusCodes.OK
          responseAs[Option[ProjectConfiguration]] shouldEqual Option(projectConfiguration)
        }
      }
    }
  }
}
