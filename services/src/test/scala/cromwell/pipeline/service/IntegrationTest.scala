package cromwell.pipeline.service

import java.nio.file.{Path, Paths}

import cromwell.pipeline.datastorage.dto.{Project, ProjectId, UserId, Version}
import org.scalatest.FlatSpec

import scala.concurrent.Await
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._
import scala.language.postfixOps
import org.scalatest.{FlatSpec, Matchers}

class IntegrationTest extends FlatSpec with Matchers {

  "An empty Set" should "have size 0" in {
    println("hi ???")
    val httpClient = new AkkaHttpClient()
    val gitLabService = new GitLabProjectVersioning(httpClient)
    val project =
      Project(ProjectId("17792592"), UserId("IaroslavTavchenkov"), "name-17673061", "repository-17673061", false)

//    val result = Await.result(gitLabService.createRepository(project), 10 second)
//    println(result)

    val fileData = Await.result(gitLabService.getFile(project, Paths.get("test.md"), Some(Version("master") )), 10 second)
    println(fileData)
    1 shouldBe (1)
  }
}
