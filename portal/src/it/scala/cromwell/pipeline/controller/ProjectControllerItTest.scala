package cromwell.pipeline.controller

import java.util.UUID

import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.testkit.ScalatestRouteTest
import com.dimafeng.testcontainers.{ ForAllTestContainer, PostgreSQLContainer }
import com.typesafe.config.Config
import cromwell.pipeline.ApplicationComponents
import cromwell.pipeline.datastorage.dto.{ Project, ProjectCreationRequest, ProjectId, User }
import cromwell.pipeline.utils.auth.{ AccessTokenContent, TestContainersUtils, TestUserUtils }
import de.heikoseeberger.akkahttpplayjson.PlayJsonSupport._
import org.scalatest.concurrent.ScalaFutures._
import org.scalatest.time.{ Millis, Seconds, Span }
import org.scalatest.{ AsyncWordSpec, Matchers }
import org.scalatestplus.mockito.MockitoSugar

class ProjectControllerItTest
    extends AsyncWordSpec
    with Matchers
    with MockitoSugar
    with ScalatestRouteTest
    with ForAllTestContainer {

  override val container: PostgreSQLContainer = TestContainersUtils.getPostgreSQLContainer()
  container.start()
  implicit val config: Config = TestContainersUtils.getConfigForPgContainer(container)
  private val components: ApplicationComponents = new ApplicationComponents()

  implicit val patienceConfig: PatienceConfig =
    PatienceConfig(timeout = Span(5, Seconds), interval = Span(500, Millis))

  override protected def beforeAll(): Unit =
    components.datastorageModule.pipelineDatabaseEngine.updateSchema()

  import components.controllerModule.projectController
  import components.datastorageModule.{ projectRepository, userRepository }

  private val userPassword = "-Pa$$w0rd-"

  "ProjetController" when {

    "GET project by id" should {
      "return project entity" in {
        val dummyUser: User = TestUserUtils.getDummyUser(password = userPassword)
        val dummyProject = TestUserUtils.getDummyProject(dummyUser.userId)
        val accessToken = AccessTokenContent(dummyUser.userId.value)

        for {
          _ <- userRepository.addUser(dummyUser)
          _ <- projectRepository.addProject(dummyProject)
        } yield {
          Get(s"/projects/${dummyProject.projectId.value}") ~> projectController.route(accessToken) ~> check {
            status shouldBe StatusCodes.OK
            responseAs[Project] shouldBe dummyProject
          }
        }
      }

      "return '404 Not Found' if there is no project with such id" in {
        val dummyUser: User = TestUserUtils.getDummyUser(password = userPassword)
        val nonExisitingProjectId = UUID.randomUUID().toString
        val accessToken = AccessTokenContent(dummyUser.userId.value)

        userRepository.addUser(dummyUser).map { _ =>
          Get(s"/projects/$nonExisitingProjectId") ~> projectController.route(accessToken) ~> check {
            status shouldBe StatusCodes.NotFound
          }
        }
      }

    }

    "DELETE project by id" should {
      "return deactivated project entity" in {
        val dummyUser: User = TestUserUtils.getDummyUser()
        val dummyProject = TestUserUtils.getDummyProject(dummyUser.userId)
        val accessToken = AccessTokenContent(dummyUser.userId.value)
        val deactivatedProject = dummyProject.copy(active = false)

        for {
          _ <- userRepository.addUser(dummyUser)
          _ <- projectRepository.addProject(dummyProject)
        } yield {
          Delete(s"/projects/${dummyProject.projectId.value}") ~> projectController.route(accessToken) ~> check {
            status shouldBe StatusCodes.OK
            responseAs[Project] shouldBe deactivatedProject
          }
        }
      }

      "return '404 Not Found' if there is no project with such id" in {
        val dummyUser: User = TestUserUtils.getDummyUser(password = userPassword)
        val nonExisitingProjectId = UUID.randomUUID().toString
        val accessToken = AccessTokenContent(dummyUser.userId.value)

        userRepository.addUser(dummyUser).map { _ =>
          Delete(s"/projects/$nonExisitingProjectId") ~> projectController.route(accessToken) ~> check {
            status shouldBe StatusCodes.NotFound
          }
        }
      }

      "return '403 Forbidden' if user is not an owner of that project" in {
        val user: User = TestUserUtils.getDummyUser()
        val anotherUser: User = TestUserUtils.getDummyUser()

        val dummyProject = TestUserUtils.getDummyProject(anotherUser.userId)
        val accessToken = AccessTokenContent(user.userId.value)

        for {
          _ <- userRepository.addUser(user)
          _ <- userRepository.addUser(anotherUser)
          _ <- projectRepository.addProject(dummyProject)
        } yield {
          Delete(s"/projects/${dummyProject.projectId.value}") ~> projectController.route(accessToken) ~> check {
            status shouldBe StatusCodes.Forbidden
            responseAs[String] shouldBe ProjectController.PROJECT_DEACTIVATION_FORBIDDEN_MESSAGE
          }
        }
      }
    }

    "POST new project" should {
      "return id of added project" in {
        val dummyUser: User = TestUserUtils.getDummyUser(active = false)
        val dummyProject = TestUserUtils.getDummyProject(dummyUser.userId)
        val projectCreationRequest = ProjectCreationRequest(
          ownerId = dummyUser.userId,
          name = dummyProject.name,
          repository = dummyProject.repository
        )
        val accessToken = AccessTokenContent(dummyUser.userId.value)

        userRepository.addUser(dummyUser).map { _ =>
          var projectUuid = ""
          Post(s"/projects/", projectCreationRequest) ~> projectController.route(accessToken) ~> check {
            status shouldBe StatusCodes.OK
            projectUuid = responseAs[String]
          }
          Get(s"/projects/$projectUuid") ~> projectController.route(accessToken) ~> check {
            status shouldBe StatusCodes.OK
            responseAs[Project] shouldBe dummyProject.copy(projectId = ProjectId(projectUuid))
          }
        }
      }
    }

  }
}
