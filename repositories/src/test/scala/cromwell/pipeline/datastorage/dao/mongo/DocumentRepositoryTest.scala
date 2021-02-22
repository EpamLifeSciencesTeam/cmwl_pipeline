package cromwell.pipeline.datastorage.dao.mongo

import org.mockito.Mockito.when
import org.mongodb.scala.result.UpdateResult
import org.mongodb.scala.{ Document, MongoCollection }
import org.scalatest.{ AsyncWordSpec, Matchers }
import org.scalatestplus.mockito.MockitoSugar

class DocumentRepositoryTest extends AsyncWordSpec with Matchers with MockitoSugar {

  private val updateResult = mock[UpdateResult]
  private val collection = mock[MongoCollection[Document]]

  private val documentRepository = new DocumentRepository(collection)

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
