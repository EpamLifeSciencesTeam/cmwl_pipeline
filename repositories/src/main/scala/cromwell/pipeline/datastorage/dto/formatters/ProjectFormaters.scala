package cromwell.pipeline.datastorage.dto.formatters

import cromwell.pipeline.datastorage.dto.{ ProjectAdditionRequest, ProjectDeleteRequest, ProjectUpdateRequest }
import play.api.libs.json.{ Json, OFormat }

object ProjectFormaters {
  implicit lazy val projectAdditionFormat: OFormat[ProjectAdditionRequest] = Json.format[ProjectAdditionRequest]
  implicit lazy val projectDeleteFormat: OFormat[ProjectDeleteRequest] = Json.format[ProjectDeleteRequest]
  implicit val updateRequestFormat: OFormat[ProjectUpdateRequest] = Json.format[ProjectUpdateRequest]
}
