package cromwell.pipeline.service.impls

import cromwell.pipeline.datastorage.dto._
import cromwell.pipeline.service.AggregationService

import scala.concurrent.Future

class AggregationServiceTestImpl(cromwellInput: Seq[CromwellInput], testMode: TestMode) extends AggregationService {

  override def aggregate(run: Run): Future[CromwellInput] =
    testMode match {
      case WithException(exc) => Future.failed(exc)
      case _                  => Future.successful(cromwellInput.head)
    }

}

object AggregationServiceTestImpl {

  def apply(cromwellInput: CromwellInput*): AggregationServiceTestImpl =
    new AggregationServiceTestImpl(cromwellInput = cromwellInput, testMode = Success)

  def withException(exception: Throwable): AggregationServiceTestImpl =
    new AggregationServiceTestImpl(cromwellInput = Seq.empty, testMode = WithException(exception))

}
