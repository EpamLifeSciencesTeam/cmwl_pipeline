package cromwell.pipeline.utils

import play.api.libs.json.{ Json, OFormat }

case class DummyObject(someId: Int, someString: String)

object DummyObject {
  implicit lazy val dummyObjectFormat: OFormat[DummyObject] = Json.format[DummyObject]
}
