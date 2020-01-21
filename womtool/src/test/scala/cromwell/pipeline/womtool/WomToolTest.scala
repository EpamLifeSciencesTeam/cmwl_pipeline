package cromwell.pipeline.womtool

import cats.data.NonEmptyList
import cromwell.languages.LanguageFactory
import cromwell.languages.util.ImportResolver.{ DirectoryResolver, HttpResolver, ImportResolver }
import org.scalatest.{ Matchers, WordSpec }
import wom.executable.WomBundle

class WomToolTest extends WordSpec with Matchers {

  val womTool: WomToolAPI = new WomTool

  lazy val importResolvers: List[ImportResolver] =
    DirectoryResolver.localFilesystemResolvers(None) :+ HttpResolver(relativeTo = None)

  val correctInputsAnswer =
    """{
      |  "test.hello.name": "String"
      |}
      |""".stripMargin

  val correctValidateAnswer = """[Task name=hello commandTemplate=Vector(
                                |    echo 'Hello world!'
                                |  )]""".stripMargin

  val inCorrectInputs = """NonEmptyList(ERROR: Call references a task (hello) that doesn't exist (line 14, col 8)
                          |
                          |  call hello
                          |       ^
                          |     )""".stripMargin

  val correctWdl =
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

  val inCorrectWdl =
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

        val res: Either[NonEmptyList[String], (WomBundle, LanguageFactory)] =
          womTool.validate(correctWdl, importResolvers)

        res.right.get._1.allCallables("hello").toString.stripMargin should be(correctValidateAnswer)

      }
      "return the error message" in {

        val res: Either[NonEmptyList[String], (WomBundle, LanguageFactory)] =
          womTool.validate(inCorrectWdl, importResolvers)

        res.left.get.head.slice(0, 5) should be("ERROR")
      }
    }

    "get an inputs from wdl" should {

      "return the correct input json" in {

        val res: Either[NonEmptyList[String], String] =
          womTool.inputs(correctWdl)

        res.right.get should be(correctInputsAnswer)
      }

      "return the ERROR" in {

        val res: Either[NonEmptyList[String], String] =
          womTool.inputs(inCorrectWdl)

        res.left.get.toString.stripMargin should be(inCorrectInputs)
      }
    }
  }
}
