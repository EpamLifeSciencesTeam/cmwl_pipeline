package cromwell.pipeline.controller.utils

import akka.http.scaladsl.unmarshalling.Unmarshaller
import cats.data.Validated
import cromwell.pipeline.model.wrapper.RunId

object FromStringUnmarshallers {

  implicit val stringToRunId: Unmarshaller[String, RunId] = Unmarshaller.strict[String, RunId] { runId =>
    RunId.from(runId) match {
      case Validated.Valid(content)  => content
      case Validated.Invalid(errors) => throw new RuntimeException(errors.head)
    }
  }
}
