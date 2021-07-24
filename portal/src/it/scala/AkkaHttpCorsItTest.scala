import akka.http.scaladsl.model.ContentTypes.`application/json`
import akka.http.scaladsl.model.headers.{ HttpOrigin, Origin, `Access-Control-Expose-Headers` }
import akka.http.scaladsl.model.{ HttpEntity, StatusCodes }
import akka.http.scaladsl.testkit.ScalatestRouteTest
import ch.megard.akka.http.cors.scaladsl.CorsRejection
import ch.megard.akka.http.cors.scaladsl.CorsRejection.InvalidOrigin
import com.dimafeng.testcontainers.{ ForAllTestContainer, PostgreSQLContainer }
import com.typesafe.config.Config
import cromwell.pipeline.controller.AuthController._
import cromwell.pipeline.datastorage.dao.utils.TestUserUtils
import cromwell.pipeline.datastorage.dao.utils.TestUserUtils.userPassword
import cromwell.pipeline.datastorage.dto.UserWithCredentials
import cromwell.pipeline.utils.TestContainersUtils
import cromwell.pipeline.{ ApplicationComponents, CromwellPipelineRoute }
import org.scalatest.{ Matchers, WordSpec }

class AkkaHttpCorsItTest extends WordSpec with Matchers with ScalatestRouteTest with ForAllTestContainer {

  override val container: PostgreSQLContainer = TestContainersUtils.getPostgreSQLContainer()
  private implicit lazy val config: Config = TestContainersUtils.getConfigForPgContainer(container)
  private lazy val components: ApplicationComponents = new ApplicationComponents()
  private lazy val route = new CromwellPipelineRoute(components.applicationConfig, components.controllerModule).route

  private val dummyUser: UserWithCredentials = TestUserUtils.getDummyUserWithCredentials()
  private val signUpRequestStr =
    s"""{"email":"${dummyUser.email}","password":"$userPassword","firstName":"${dummyUser.firstName}","lastName":"${dummyUser.lastName}"}"""
  private val httpEntity: HttpEntity.Strict = HttpEntity(`application/json`, signUpRequestStr)
  private lazy val allowedOrigin: String = components.applicationConfig.webServiceConfig.cors.allowedOrigins.head

  override protected def beforeAll(): Unit = {
    super.beforeAll()
    components.datastorageModule.pipelineDatabaseEngine.updateSchema()
  }

  "CORS" should {
    "allow requests with allowed origin" in {
      Post("/auth/signUp", httpEntity) ~> Origin(HttpOrigin(allowedOrigin)) ~> route ~> check {
        status shouldBe StatusCodes.OK
      }
    }

    "reject requests with not allowed origin" in {
      val notAllowedOrigin = "http://not_allowed_origin:3000"

      Post("/auth/signUp", httpEntity) ~> Origin(HttpOrigin(notAllowedOrigin)) ~> route ~> check {
        rejection shouldEqual CorsRejection(InvalidOrigin(List(notAllowedOrigin)))
      }
    }

    "expose auth headers" in {
      val authHeaders = List(AccessTokenHeader, RefreshTokenHeader, AccessTokenExpirationHeader)

      Post("/auth/signUp", httpEntity) ~> Origin(HttpOrigin(allowedOrigin)) ~> route ~> check {
        response.headers should contain(`Access-Control-Expose-Headers`(authHeaders))
      }
    }
  }

}
