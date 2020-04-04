package cromwell.pipeline.datastorage.dto.auth

final case class PasswordProblemsResponse(value: String, isValid: Boolean, errors: List[Map[String, String]])
