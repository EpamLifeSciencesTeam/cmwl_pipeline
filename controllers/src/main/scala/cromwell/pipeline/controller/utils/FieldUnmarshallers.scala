package cromwell.pipeline.controller.utils

import akka.http.scaladsl.common.StrictForm.Field
import akka.http.scaladsl.unmarshalling.FromStrictFormFieldUnmarshaller
import cromwell.pipeline.controller.utils.FromStringUnmarshallers._
import cromwell.pipeline.datastorage.dto.PipelineVersion

import java.nio.file.Path

object FieldUnmarshallers {
  implicit val pipelineVersionFieldUnmarshaller: FromStrictFormFieldUnmarshaller[PipelineVersion] =
    Field.unmarshallerFromFSU(stringToPipelineVersion)
  implicit val pathFieldUnmarshaller: FromStrictFormFieldUnmarshaller[Path] = Field.unmarshallerFromFSU(stringToPath)
}
