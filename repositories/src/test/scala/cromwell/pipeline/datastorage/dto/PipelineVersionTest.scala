package cromwell.pipeline.datastorage.dto

import cromwell.pipeline.datastorage.dto.PipelineVersion.PipelineVersionException
import org.scalatest.{ Matchers, WordSpec }

class PipelineVersionTest extends WordSpec with Matchers {
  "PipelineVersion" when {

    "apply method" should {
      "return correct PipelineVersion" taggedAs Dto in {
        val version = PipelineVersion("v1.2.3")
        val expectedName = "v1.2.3"

        version.name shouldBe expectedName
        version.major.unwrap shouldBe 1
        version.minor.unwrap shouldBe 2
        version.revision.unwrap shouldBe 3
      }

      "throw PipelineVersionException due to invalid constructor parameter in apply method" taggedAs Dto in {
        val versionLine = "1.2"
        val caught = intercept[PipelineVersionException] {
          PipelineVersion(versionLine)
        }

        caught.message shouldBe s"Format of version name: 'v(int).(int).(int)', but got: $versionLine"
      }

    }

    "increasing of major member" should {
      "increase major and reset minor with revision members" taggedAs Dto in {
        val version = PipelineVersion("v1.2.3")
        val updatedVersion = PipelineVersion("v2.0.0")

        version.increaseMajor shouldBe updatedVersion
      }
    }

    "increasing of minor member" should {
      "increase minor and reset revision members" taggedAs Dto in {
        val version = PipelineVersion("v1.2.3")
        val updatedVersion = PipelineVersion("v1.3.0")

        version.increaseMinor shouldBe updatedVersion
      }
    }

    "increasing of revision member" should {
      "increase revision member" taggedAs Dto in {
        val version = PipelineVersion("v1.2.3")
        val updatedVersion = PipelineVersion("v1.2.4")

        version.increaseRevision shouldBe updatedVersion
      }
    }
  }
}
