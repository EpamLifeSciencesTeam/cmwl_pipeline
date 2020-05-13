package cromwell.pipeline.controller

import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import cromwell.pipeline.datastorage.dto.{ FileContent, ProjectUpdateFileRequest }
import cromwell.pipeline.datastorage.utils.auth.AccessTokenContent
import cromwell.pipeline.service.ProjectFileService
import de.heikoseeberger.akkahttpplayjson.PlayJsonSupport._

import scala.concurrent.ExecutionContext
import scala.util.{ Failure, Success }

class ProjectFileController(wdlService: ProjectFileService)(implicit val executionContext: ExecutionContext) {
  val route: AccessTokenContent => Route = _ =>
    concat(
      path("files" / "validation") {
        post {
          entity(as[FileContent]) { request =>
            onComplete(wdlService.validateFile(request)) {
              case Success(Left(e)) => complete(StatusCodes.Conflict, e.errors)
              case Success(_)       => complete(StatusCodes.OK)
              case Failure(e)       => complete(StatusCodes.InternalServerError, e.getMessage)
            }
          }
        }
      },
      path("files") {
        post {
          entity(as[ProjectUpdateFileRequest]) {
            request =>
              val result = for {
                validateResponse <- wdlService.validateFile(FileContent(request.projectFile.content))
                uploadResponse <- wdlService.uploadFile(request.project, request.projectFile)
              } yield {
                if (validateResponse.isRight && uploadResponse.isRight) {
                  StatusCodes.OK.intValue -> uploadResponse.right.get
                } else if (validateResponse.isLeft && uploadResponse.isRight) {
                  StatusCodes.Created.intValue -> uploadResponse.right.get
                } else if (uploadResponse.isLeft) {
                  StatusCodes.ImATeapot.intValue -> uploadResponse.left.get.message
                }
              }

              while (!result.isCompleted) {}

              result.value.get match {
                case Success(value) =>
                  value match {
                    case tuple @ (status: Int, message: String) => complete(status, message)
                  }
                case Failure(e) => complete(StatusCodes.InternalServerError, e.getMessage)
              }
          }
        }
      }
    )
}
