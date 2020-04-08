package cromwell.pipeline.service

import cats.data.NonEmptyList
import cromwell.pipeline.datastorage.dto.{ FileContent, ValidationError }
import cromwell.pipeline.womtool.WomTool
import org.mockito.Mockito.when
import org.mockito.Matchers.any
import org.scalatest.{ AsyncWordSpec, Matchers }
import org.scalatestplus.mockito.MockitoSugar
import wom.executable.WomBundle

class ProjectFileServiceTest extends AsyncWordSpec with Matchers with MockitoSugar {

  private val womTool = mock[WomTool]
  private val projectFileService = new ProjectFileService(womTool)

  private val correctWdl = "task hello {}"
  private val incorrectWdl = "task hello {"
  private val errorMessage = "ERROR: miss bracket"

  "ProjectFileServiceTest" when {
    "validateFile" should {
      "return valid message to valid file" taggedAs Service in {
        val request = FileContent(correctWdl)

        when(womTool.validate(correctWdl)).thenReturn(Right(any[WomBundle]))
        projectFileService.validateFile(request).map(_ shouldBe Right(()))
      }

      "return error message to incorrect file" taggedAs Service in {
        val request = FileContent(incorrectWdl)

        when(womTool.validate(incorrectWdl)).thenReturn(Left(NonEmptyList(errorMessage, Nil)))
        projectFileService.validateFile(request).map(_ shouldBe Left(ValidationError(List(errorMessage))))
      }
    }
  }
}
