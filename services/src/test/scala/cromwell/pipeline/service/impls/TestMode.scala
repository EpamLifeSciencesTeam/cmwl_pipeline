package cromwell.pipeline.service.impls

sealed trait TestMode

final case class WithException(exc: Throwable) extends TestMode
case object Success extends TestMode
