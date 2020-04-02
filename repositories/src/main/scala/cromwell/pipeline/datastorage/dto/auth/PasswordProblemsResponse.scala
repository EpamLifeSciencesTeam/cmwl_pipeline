package cromwell.pipeline.datastorage.dto.auth

final case class PasswordProblemsResponse(value: String, valid: Boolean = false, errors: List[Map[String, String]])
