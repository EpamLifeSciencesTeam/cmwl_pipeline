package cromwell.pipeline.service

sealed abstract class CromwellIntegrationException(message: String) extends Exception(message)

object CromwellIntegrationException {
  case class ServerStatusException(message: String) extends CromwellIntegrationException(message)
}
