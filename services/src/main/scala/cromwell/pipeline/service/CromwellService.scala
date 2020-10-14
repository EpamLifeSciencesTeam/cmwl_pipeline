package cromwell.pipeline.service

import cromwell.pipeline.utils.{CromwellConfig, HttpStatusCodes}
import play.api.libs.json.JsObject

import scala.concurrent.{ExecutionContext, Future}

class CromwellService(httpClient: HttpClient, cromwellConfig: CromwellConfig)(
  implicit executionContext: ExecutionContext
) {
  def getEngineStatus(): Future[Either[Exception, Int]] = {
    val awsUrl: String = cromwellConfig.host + cromwellConfig.enginePath + "/" + cromwellConfig.version + "/status"
    httpClient.get[JsObject](url = awsUrl).map {
      case Response(HttpStatusCodes.OK, SuccessResponseBody(_), _) =>
        Right(HttpStatusCodes.OK)
      case Response(status, FailureResponseBody(body), _) =>
        Left(CromwellIntegrationException.ServerStatusException(s"Could not parse to json. Response status: ${status}. Response body: ${body}"))
      case Response(status, _, _) =>
        Left(CromwellIntegrationException.ServerStatusException(s"Exception. Response status: ${status}."))
    }
  }
}
