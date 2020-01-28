package cromwell.pipeline.controller

import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.testkit.ScalatestRouteTest
import com.dimafeng.testcontainers.{ForAllTestContainer, PostgreSQLContainer}
import com.typesafe.config.Config
import cromwell.pipeline.ApplicationComponents
import cromwell.pipeline.datastorage.dto.{Project, ProjectId, User, UserId}
import cromwell.pipeline.utils.auth.{AccessTokenContent, TestContainersUtils, TestProjectUtils, TestUserUtils}
import de.heikoseeberger.akkahttpplayjson.PlayJsonSupport
import org.scalatest.{AsyncWordSpec, Matchers}

class ProjectControllerTest
  extends AsyncWordSpec
    with Matchers
    with ScalatestRouteTest
    with PlayJsonSupport
    with ForAllTestContainer {

  override val container: PostgreSQLContainer = TestContainersUtils.getPostgreSQLContainer()
  container.start()
  implicit val config: Config = TestContainersUtils.getConfigForPgContainer(container)
  private val components: ApplicationComponents = new ApplicationComponents()
  override protected def beforeAll(): Unit = components.datastorageModule.pipelineDatabaseEngine.updateSchema()

  import components.controllerModule.projectController
  import components.datastorageModule.projectRepository
  import components.controllerModule.userController
  import components.datastorageModule.userRepository

  "ProjectController" when {

    "getProjectByName" should {
      "return a project with the same name" in {
        val dummyUser = TestUserUtils.getDummyUser("1")
        val dummyProject = TestProjectUtils.getDummyProject("11","1")
        val projectByNameRequest = dummyProject.name
        val optionProject: Option[Project] = Option(dummyProject)
        userRepository.addUser(dummyUser)
        projectRepository.addProject(dummyProject).map { _ =>
          val accessToken = AccessTokenContent(dummyProject.ownerId.value)
          Get("/projects?name=" + projectByNameRequest) ~> projectController.route(accessToken) ~> check {
            status shouldBe StatusCodes.OK
            responseAs[Option[Project]] shouldEqual optionProject
          }
        }
      }
    }

  }

}
