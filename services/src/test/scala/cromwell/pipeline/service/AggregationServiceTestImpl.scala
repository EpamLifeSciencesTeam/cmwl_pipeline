package cromwell.pipeline.service
import cromwell.pipeline.datastorage.dto._

import scala.concurrent.Future

class AggregationServiceTestImpl(cromwellInput: Seq[CromwellInput], testMode: TestMode) extends AggregationService {

  override def aggregate(run: Run): Future[CromwellInput] =
    testMode match {
      case WithException(exc) => Future.failed(exc)
      case _                  => Future.successful(cromwellInput.head)
    }

}

object AggregationServiceTestImpl {
  def apply(cromwellInput: CromwellInput*)(implicit testMode: TestMode = Success): AggregationServiceTestImpl =
    new AggregationServiceTestImpl(cromwellInput, testMode)

}
