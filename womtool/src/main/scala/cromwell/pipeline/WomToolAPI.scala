package cromwell.pipeline

trait WomToolAPI {

  def validate(path: String)

  def generateJsonFromParams()
}
