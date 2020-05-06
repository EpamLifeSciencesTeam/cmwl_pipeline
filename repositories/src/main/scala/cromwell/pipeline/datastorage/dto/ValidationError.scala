package cromwell.pipeline.datastorage.dto

final case class ValidationError(errors: List[String]) extends Exception
