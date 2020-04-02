package cromwell.pipeline.datastorage.dto.auth

final case class SignUpRequest(email: String, password: String, firstName: String, lastName: String)
