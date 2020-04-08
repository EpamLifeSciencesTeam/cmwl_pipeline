package cromwell.pipeline.datastorage.dto

case class ValidationError(errors: List[String]) extends Exception
