package cromwell.pipeline.service
import cromwell.pipeline.datastorage.dto._

import scala.concurrent.Future

class AggregationServiceTestImpl(valToReturn: DummyToReturn) extends AggregationService {

  override def aggregate(run: Run): Future[CromwellInput] =
    valToReturn match {
      case WithException(exc)                   => Future.failed(exc)
      case CromwellInputToReturn(cromwellInput) => Future.successful(cromwellInput)
      case _                                    => throw new RuntimeException("Wrong returned test value type")
    }

}

object AggregationServiceTestImpl {
  def apply(valToReturn: DummyToReturn): AggregationServiceTestImpl =
    new AggregationServiceTestImpl(valToReturn)

}
