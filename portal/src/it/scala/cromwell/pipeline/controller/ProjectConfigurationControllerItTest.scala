package cromwell.pipeline.controller

import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.testkit.ScalatestRouteTest
import com.dimafeng.testcontainers.{ ForAllTestContainer, MongoDBContainer }
import com.typesafe.config.Config
import cromwell.pipeline.ApplicationComponents
import cromwell.pipeline.utils.TestContainersUtils
import de.heikoseeberger.akkahttpplayjson.PlayJsonSupport
import org.scalatest.{ AsyncWordSpec, Matchers }
import cromwell.pipeline.datastorage.dao.repository.DocumentRepository
import org.mongodb.scala.{ Document, MongoClient }
import com.osinka.subset._
import cromwell.pipeline.datastorage.dao.repository.utils.{
  TestProjectConfigurationUtils,
  TestProjectUtils,
  TestUserUtils
}
import cromwell.pipeline.datastorage.dto.ProjectConfiguration.toDocument
import cromwell.pipeline.datastorage.dto.auth.AccessTokenContent
import cromwell.pipeline.datastorage.dto.{ ProjectConfiguration, User }

class ProjectConfigurationControllerItTest
    extends AsyncWordSpec
    with Matchers
    with ScalatestRouteTest
    with PlayJsonSupport
    with ForAllTestContainer {

  // TODO cc core vs mongo versions
  override def container: MongoDBContainer = TestContainersUtils.getMongoDBContainer()
  implicit lazy val config: Config = TestContainersUtils.getConfigForMongoDBContainer(container)
  private lazy val components: ApplicationComponents = new ApplicationComponents()

  import components.controllerModule.configurationController
  import components.datastorageModule.configurationRepository

  val projectConfiguration = TestProjectConfigurationUtils.projectConfigurationGen.sample.get
  val doc = ProjectConfiguration.toDocument(projectConfiguration)
  val id = projectConfiguration.projectId

  override protected def beforeAll(): Unit = {
    super.beforeAll()
    components.datastorageModule.pipelineDatabaseEngine.updateSchema()
    val x = configurationRepository.addOne(doc)
  }

  "project configuration controller" when {
    "add some project configuration" should {
      "return OK status code" in {
        val dummyUser: User = TestUserUtils.getDummyUser()
        val dummyProject = TestProjectUtils.getDummyProject()
        val projectConfiguration = ProjectConfiguration(dummyProject.projectId, List())
        val accessToken = AccessTokenContent(dummyUser.userId)
        configurationRepository.addOne(toDocument(projectConfiguration)).flatMap { _ =>
          Get("/configurations?=" + dummyProject.projectId) ~> configurationController.route(accessToken) ~> check {
            status shouldBe StatusCodes.OK
          }
        }
      }
    }
  }
}
