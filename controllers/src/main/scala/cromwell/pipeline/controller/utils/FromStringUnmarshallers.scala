package cromwell.pipeline.controller.utils

import akka.http.scaladsl.unmarshalling.Unmarshaller
import cats.data.Validated
import cromwell.pipeline.datastorage.dto.PipelineVersion
import cromwell.pipeline.model.validator.Enable
import cromwell.pipeline.model.wrapper.{ ProjectId, RunId }

import java.nio.file.{ Path, Paths }

object FromStringUnmarshallers {

  implicit val stringToRunId: Unmarshaller[String, RunId] = Unmarshaller.strict[String, RunId] { runId =>
    RunId.from(runId) match {
      case Validated.Valid(content)  => content
      case Validated.Invalid(errors) => throw new RuntimeException(errors.head)
    }
  }
  implicit val stringToProjectId: Unmarshaller[String, ProjectId] = Unmarshaller.strict[String, ProjectId] {
    projectId =>
      ProjectId(projectId, Enable.Unsafe)
  }
  implicit val stringToPath: Unmarshaller[String, Path] = Unmarshaller.strict[String, Path] { path =>
    Paths.get(path)
  }
  implicit val stringToPipelineVersion: Unmarshaller[String, PipelineVersion] =
    Unmarshaller.strict[String, PipelineVersion] { version =>
      PipelineVersion(version)
    }
}
