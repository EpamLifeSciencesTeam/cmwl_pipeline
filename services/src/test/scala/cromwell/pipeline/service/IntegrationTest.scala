package cromwell.pipeline.service

import cromwell.pipeline.datastorage.dto.{ Project, ProjectId, UserId }
import org.scalatest.FlatSpec
import scala.concurrent.Await
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._
import scala.language.postfixOps

import org.scalatest.{ FlatSpec, Matchers }

class IntegrationTest extends FlatSpec with Matchers {

  "An empty Set" should "have size 0" in {
    println("hi ???")
    val httpClient = new AkkaHttpClient()
    val gitLabService = new GitLabProjectVersioning(httpClient)
    val project =
      Project(ProjectId("17603694"), UserId("IaroslavTavchenkov"), "name-17603694", "repository-17603694", false)
    val result = Await.result(gitLabService.createRepository(project), 10 second)
    println(result)
    1 shouldBe (1)
  }
}
