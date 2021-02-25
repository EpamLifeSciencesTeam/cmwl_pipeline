package cromwell.pipeline.datastorage.dao.mongo

import cromwell.pipeline.datastorage.dao.mongo.DocumentCodecInstances._
import cromwell.pipeline.datastorage.dao.mongo.DocumentDecoder.DocumentDecoderSyntax
import cromwell.pipeline.datastorage.dao.mongo.DocumentEncoder.DocumentEncoderSyntax
import cromwell.pipeline.datastorage.dao.utils.ArbitraryUtils._
import cromwell.pipeline.datastorage.dto.ProjectConfiguration
import org.scalatest.{ Matchers, WordSpec }
import org.scalatestplus.scalacheck.ScalaCheckDrivenPropertyChecks

class DocumentCodecTest extends WordSpec with Matchers with ScalaCheckDrivenPropertyChecks {

  "DocumentCodec" should {
    "be able to convert to document and from document" when {
      "converting ProjectConfiguration" in {
        forAll { (configuration: ProjectConfiguration) =>
          val document = configuration.toDocument
          val parsedConfiguration = document.fromDocument[ProjectConfiguration]

          parsedConfiguration shouldBe configuration
        }
      }
    }
  }

}
