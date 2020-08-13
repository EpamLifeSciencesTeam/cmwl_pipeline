package cromwell.pipeline.datastorage.dto

object File {
  case class UpdateFileRequest(content: ProjectFileContent, commitMessage: String, branch: String)
}
