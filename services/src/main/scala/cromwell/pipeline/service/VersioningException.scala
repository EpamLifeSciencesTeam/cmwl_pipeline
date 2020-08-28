package cromwell.pipeline.service

sealed abstract class VersioningException(message: String) extends Exception(message)

object VersioningException {

  case class ProjectException(message: String) extends VersioningException(message)
  case class FileException(message: String) extends VersioningException(message)
  case class GitException(message: String) extends VersioningException(message)
  case class HttpException(message: String) extends VersioningException(message)
  case class RepositoryException(message: String) extends VersioningException(message)

}
