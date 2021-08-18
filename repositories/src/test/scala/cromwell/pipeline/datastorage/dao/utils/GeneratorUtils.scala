package cromwell.pipeline.datastorage.dao.utils

import cats.implicits._
import cromwell.pipeline.datastorage.dto._
import cromwell.pipeline.datastorage.dto.auth.{ SignInRequest, SignUpRequest }
import cromwell.pipeline.datastorage.dto.user.{ PasswordUpdateRequest, UserUpdateRequest }
import cromwell.pipeline.model.validator.Enable
import cromwell.pipeline.model.wrapper._
import org.scalacheck.Gen
import java.nio.file.{ Path, Paths }
import scala.util.Random

object GeneratorUtils {
  private val defaultStrLength = 10
  private val defaultListMaxLength = 10

  private def stringGen(n: Int = defaultStrLength): Gen[String] = Gen.listOfN(n, Gen.alphaLowerChar).map(_.mkString)
  private val booleanGen: Gen[Boolean] = Gen.oneOf(false, true)

  private def listOfN[T](gen: Gen[T], maxLength: Int = defaultListMaxLength): Gen[List[T]] =
    Gen.chooseNum(0, maxLength).flatMap(length => Gen.listOfN(length, gen))

  private lazy val projectIdGen: Gen[ProjectId] = Gen.uuid.map(id => ProjectId(id.toString))

  private lazy val projectConfigurationIdGen: Gen[ProjectConfigurationId] =
    Gen.uuid.map(id => ProjectConfigurationId(id.toString))

  private lazy val emailGen: Gen[UserEmail] = for {
    name <- stringGen()
    mail <- Gen.oneOf(Mail.values.toSeq)
    domain <- Gen.oneOf(Domain.values.toSeq)
  } yield UserEmail(s"$name@$mail.$domain", Enable.Unsafe)

  private lazy val nameGen: Gen[Name] = for {
    name <- stringGen(6)
  } yield Name(name, Enable.Unsafe)

  private lazy val pathGen: Gen[Path] = Gen
    .choose(1, 5)
    .flatMap(length => Gen.listOfN(length, stringGen(5)).map(_.mkString("/")).map(path => Paths.get(path)))

  private lazy val versionValueGen: Gen[VersionValue] = Gen.posNum[Int].map(VersionValue(_, Enable.Unsafe))

  private lazy val pipelineVersionGen: Gen[PipelineVersion] = for {
    major <- versionValueGen
    minor <- versionValueGen
    revision <- versionValueGen
  } yield PipelineVersion(major, minor, revision)

  private lazy val projectConfigurationVersionGen: Gen[ProjectConfigurationVersion] = for {
    version <- versionValueGen
  } yield ProjectConfigurationVersion(version)

  private lazy val passwordGen: Gen[Password] = for {
    upperCase <- Gen.alphaUpperStr.suchThat(s => s.nonEmpty)
    lowerLetters <- stringGen()
    digits <- Gen.posNum[Int]
    symbol <- Gen.oneOf(Symbol.values.toSeq)
  } yield {
    val password = Random.shuffle(List(upperCase, lowerLetters, digits.toString, symbol.toString).flatten).mkString
    Password(password, Enable.Unsafe)
  }

  private lazy val projectFileGen: Gen[ProjectFile] = for {
    path <- pathGen
    content <- projectFileContentGen
  } yield ProjectFile(path, content)

  lazy val userUpdateRequestGen: Gen[UserUpdateRequest] = for {
    email <- emailGen
    firstName <- nameGen
    lastName <- nameGen
  } yield UserUpdateRequest(email, firstName, lastName)

  lazy val passwordUpdateRequestGen: Gen[PasswordUpdateRequest] = for {
    currentPassword <- passwordGen
    newPassword <- passwordGen
  } yield PasswordUpdateRequest(currentPassword, newPassword, newPassword)

  lazy val signUpRequestGen: Gen[SignUpRequest] = for {
    email <- emailGen
    password <- passwordGen
    firstName <- nameGen
    lastName <- nameGen
  } yield SignUpRequest(email, password, firstName, lastName)

  lazy val signInRequestGen: Gen[SignInRequest] = for {
    email <- emailGen
    password <- passwordGen
  } yield SignInRequest(email, password)

  lazy val projectAdditionRequestGen: Gen[ProjectAdditionRequest] = stringGen().map(ProjectAdditionRequest(_))

  lazy val projectUpdateNameRequestGen: Gen[ProjectUpdateNameRequest] = for {
    name <- stringGen()
  } yield ProjectUpdateNameRequest(name)

  lazy val projectFileContentGen: Gen[ProjectFileContent] = stringGen().map(ProjectFileContent(_))

  lazy val gitLabFileContentGen: Gen[GitLabFileContent] = stringGen().map(GitLabFileContent(_))

  lazy val projectUpdateFileRequestGen: Gen[ProjectUpdateFileRequest] = for {
    projectFile <- projectFileGen
    pipelineVersion <- Gen.option(pipelineVersionGen)
  } yield ProjectUpdateFileRequest(projectFile, pipelineVersion)

  lazy val typedValueGen: Gen[TypedValue] = {
    val range = 1024

    val intGen: Gen[Int] = Gen.chooseNum[Int](-range, range)
    val floatGen: Gen[Float] = Gen.chooseNum[Float](-range, range)

    val stringTypedGen: Gen[StringTyped] = Gen.option(stringGen()).map(StringTyped)
    val fileTypedGen: Gen[FileTyped] = Gen.option(pathGen).map(FileTyped)
    val intTypedGen: Gen[IntTyped] = Gen.option(intGen).map(IntTyped)
    val floatTypedGen: Gen[FloatTyped] = Gen.option(floatGen).map(FloatTyped)
    val booleanTypedGen: Gen[BooleanTyped] = Gen.option(booleanGen).map(BooleanTyped)

    Gen.oneOf(stringTypedGen, fileTypedGen, intTypedGen, floatTypedGen, booleanTypedGen)
  }

  lazy val fileParameterGen: Gen[FileParameter] = for {
    name <- stringGen()
    typedValue <- typedValueGen
  } yield FileParameter(name, typedValue)

  lazy val wdlParamsGen: Gen[WdlParams] = for {
    path <- pathGen
    fileParameters <- listOfN(fileParameterGen)
  } yield WdlParams(path, fileParameters)

  lazy val projectConfigurationGen: Gen[ProjectConfiguration] = for {
    id <- projectConfigurationIdGen
    projectId <- projectIdGen
    active <- booleanGen
    wdlParams <- wdlParamsGen
    version <- projectConfigurationVersionGen
  } yield ProjectConfiguration(id, projectId, active, wdlParams, version)

  object Mail extends Enumeration {
    val Mail, Gmail, Epam, Yandex = Value
  }

  object Domain extends Enumeration {
    val Org, Com, Ru, It, Cz = Value
  }

  object Symbol extends Enumeration {
    val $, %, & = Value
  }
}
