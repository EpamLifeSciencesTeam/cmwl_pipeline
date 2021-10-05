package cromwell.pipeline.controller.utils

import akka.http.scaladsl.marshalling.PredefinedToResponseMarshallers.fromStatusCode
import akka.http.scaladsl.marshalling.{ Marshaller, ToEntityMarshaller, ToResponseMarshaller }
import akka.http.scaladsl.model.{ HttpEntity, StatusCodes }

object FromUnitMarshaller {
  implicit val fromUnitEntityMarshaller: ToEntityMarshaller[Unit] = Marshaller.opaque(_ => HttpEntity.Empty)
  implicit val fromUnitResponseMarshaller: ToResponseMarshaller[Unit] =
    fromStatusCode.compose(_ => StatusCodes.NoContent)
}
