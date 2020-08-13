package cromwell.pipeline.datastorage.formatters

import cromwell.pipeline.datastorage.dto.FileParameter
import play.api.libs.json.{ Json, OFormat }

object FileFormatters {
  implicit lazy val fileParameterFormat: OFormat[FileParameter] = Json.format
}
