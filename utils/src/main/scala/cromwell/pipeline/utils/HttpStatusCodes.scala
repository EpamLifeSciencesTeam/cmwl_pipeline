package cromwell.pipeline.utils

object HttpStatusCodes {
  val OK = 200
  val Created = 201
  val NoContent = 204
  val NotModified = 304
  val BadRequest = 400
  val Unauthorized = 401
  val Forbidden = 403
  val NotFound = 404
  val MethodNotAllowed = 405
  val Conflict = 409
  val PreconditionFailed = 412
  val UnprocessableEntity = 422
  val InternalServerError = 500
}
