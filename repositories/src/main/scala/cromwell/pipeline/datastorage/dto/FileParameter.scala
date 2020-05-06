package cromwell.pipeline.datastorage.dto

import play.api.libs.json.{ Json, OFormat }

final case class FileParameter(name: String, typedValue: TypedValue)

object FileParameter {
  implicit val fileParameterFormat: OFormat[FileParameter] = Json.format
}
