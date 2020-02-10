package cromwell.pipeline

import io.circe.Json

trait WomToolAPI {

  def validate(path: String)

  def generateJsonFromParams(params: Seq[String]): Json
}
