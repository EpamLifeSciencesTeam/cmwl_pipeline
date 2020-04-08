package cromwell.pipeline.womtool

import cats.data.NonEmptyList
import cromwell.languages.util.ImportResolver.{ DirectoryResolver, HttpResolver }
import org.scalatest.EitherValues._
import org.scalatest.{ Matchers, WordSpec }
import wom.executable.WomBundle

class WomToolTest extends WordSpec with Matchers {

  private lazy val importResolvers = DirectoryResolver.localFilesystemResolvers(None) :+ HttpResolver(relativeTo = None)

  private val womTool = new WomTool(importResolvers)

  private val correctInputsAnswer =
    """{
      |  "test.hello.name": "String"
      |}
      |""".stripMargin

  private val correctValidateAnswer = """[Task name=hello commandTemplate=Vector(
                                        |    echo 'Hello world!'
                                        |  )]""".stripMargin

  private val inCorrectInputs =
    """NonEmptyList(ERROR: Call references a task (hello) that doesn't exist (line 14, col 8)
      |
      |  call hello
      |       ^
      |     )""".stripMargin

  private val correctWdl =
    """
      |task hello {
      |  String name
      |
      |  command {
      |    echo 'Hello world!'
      |  }
      |  output {
      |    File response = stdout()
      |  }
      |}
      |
      |workflow test {
      |  call hello
      |}
      |""".stripMargin

  private val inCorrectWdl =
    """
      |task hessllo {
      |  String name
      |
      |  command {
      |    echo 'Hello world!'
      |  }
      |  output {
      |    File response = stdout()
      |  }
      |}
      |
      |workflow test {
      |  call hello
      |}
      |""".stripMargin

  "WomToolAPI" when {

    "get and validate womBundle" should {

      "return the correct bundle" in {

        val res: Either[NonEmptyList[String], WomBundle] =
          womTool.validate(correctWdl)

        res.right.value.allCallables("hello").toString.stripMargin shouldBe correctValidateAnswer
      }
      "return the error message" in {

        val res: Either[NonEmptyList[String], WomBundle] =
          womTool.validate(inCorrectWdl)

        res.left.value.head.slice(0, 5) shouldBe "ERROR"
      }
    }

    "get an inputs from wdl" should {

      "return the correct input json" in {

        val res: Either[NonEmptyList[String], String] =
          womTool.inputs(correctWdl)

        res.right.value shouldBe correctInputsAnswer
      }

      "return the ERROR" in {

        val res: Either[NonEmptyList[String], String] =
          womTool.inputs(inCorrectWdl)

        res.left.value.toString.stripMargin shouldBe inCorrectInputs
      }
    }
  }
}
