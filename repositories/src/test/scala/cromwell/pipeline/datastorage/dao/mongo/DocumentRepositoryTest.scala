package cromwell.pipeline.datastorage.dao.mongo

import com.dimafeng.testcontainers.{ ForAllTestContainer, MongoDBContainer }
import cromwell.pipeline.database.TestMongoEngine
import cromwell.pipeline.datastorage.dao.repository.ProjectConfigurationRepository
import cromwell.pipeline.datastorage.dao.utils.TestProjectUtils.getDummyProjectConfiguration
import cromwell.pipeline.datastorage.dto._
import cromwell.pipeline.utils.TestContainersUtils.getMongoContainer
import cromwell.pipeline.utils.{ MongoConfig, TestContainersUtils }
import org.mockito.Mockito.when
import org.mongodb.scala.result.UpdateResult
import org.mongodb.scala.{ Document, MongoCollection }
import org.scalatest.{ AsyncWordSpec, Matchers }
import org.scalatestplus.mockito.MockitoSugar

class DocumentRepositoryTest extends AsyncWordSpec with Matchers with MockitoSugar with ForAllTestContainer {

  override val container: MongoDBContainer = getMongoContainer()

  lazy implicit val config: MongoConfig = TestContainersUtils.getConfigForMongoContainer(container)

  lazy val mongoCollection: MongoCollection[Document] =
    new TestMongoEngine(config).mongoCollection
  lazy val documentRepository = new DocumentRepository(mongoCollection)
  lazy val mongoRepository: ProjectConfigurationRepository = ProjectConfigurationRepository(documentRepository)

  val projectConfiguration: ProjectConfiguration = getDummyProjectConfiguration()

  "Mongo repository getById" should {
    "return some configuration" in {
      mongoRepository
        .updateConfiguration(projectConfiguration)
        .flatMap(_ => mongoRepository.getById(projectConfiguration.id))
        .map(_ shouldBe Some(projectConfiguration))
    }
  }

  private val updateResult = mock[UpdateResult]

  "DocumentRepository" when {
    "check acknowledgement" should {
      "succeed if operation was acknowledged" in {
        val expected: Unit = ()
        when(updateResult.wasAcknowledged()).thenReturn(true)
        documentRepository.checkAcknowledgement(updateResult).map(_ shouldBe expected)
      }
      "fail if operation wasn't acknowledged" in {
        when(updateResult.wasAcknowledged()).thenReturn(false)
        documentRepository.checkAcknowledgement(updateResult).failed.map(_ shouldBe an[IllegalStateException])
      }
    }
  }

}
