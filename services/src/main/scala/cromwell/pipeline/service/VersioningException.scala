package cromwell.pipeline.service

sealed abstract class VersioningException extends Exception

object VersioningException {
   case class ProjectException(message: String) extends VersioningException
   case class FileException(message: String) extends VersioningException
   case class GitException(message: String) extends VersioningException
   case class HttpException(message: String) extends VersioningException
   case class RepositoryException(message: String) extends VersioningException
}
