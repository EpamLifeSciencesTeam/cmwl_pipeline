package cromwell.pipeline.datastorage.dto.auth

final case class PasswordProblemsResponse(value: String, errors: List[Map[String, String]])
