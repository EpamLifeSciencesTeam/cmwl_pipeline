package cromwell.pipeline.datastorage.dao.repository.utils

import java.nio.file.{ Path, Paths }

import cats.implicits._
import cromwell.pipeline.datastorage.dto._
import cromwell.pipeline.datastorage.dto.auth.{ SignInRequest, SignUpRequest }
import cromwell.pipeline.datastorage.dto.user.{ PasswordUpdateRequest, UserUpdateRequest }
import cromwell.pipeline.model.validator.Enable
import cromwell.pipeline.model.wrapper._
import org.scalacheck.Gen

import scala.util.Random

object GeneratorUtils {
  private def stringGen(n: Int): Gen[String] = Gen.listOfN(n, Gen.alphaLowerChar).map(_.mkString)

  private lazy val booleanGen: Gen[Boolean] = Gen.oneOf(List(false, true))

  private lazy val projectIdGen: Gen[ProjectId] = Gen.uuid.map(id => ProjectId(id.toString))

  private lazy val repositoryGen: Gen[Repository] = stringGen(10).map(Repository(_))

  private lazy val emailGen: Gen[UserEmail] = for {
    name <- stringGen(10)
    mail <- Gen.oneOf(Mail.values.toSeq)
    domain <- Gen.oneOf(Domain.values.toSeq)
  } yield UserEmail(s"$name@$mail.$domain", Enable.Unsafe)

  private lazy val nameGen: Gen[Name] = for {
    name <- stringGen(6)
  } yield Name(name, Enable.Unsafe)

  private lazy val pathGen: Gen[Path] = Gen
    .choose(1, 5)
    .flatMap(length => Gen.listOfN(length, stringGen(5)).map(_.mkString("/")).map(path => Paths.get(path)))

  private lazy val pipelineVersionGen: Gen[PipelineVersion] =
    new PipelineVersion(VersionValue.random, VersionValue.random, VersionValue.random)

  private lazy val passwordGen: Gen[Password] = for {
    upperCase <- Gen.alphaUpperStr.suchThat(s => s.nonEmpty)
    lowerLetters <- stringGen(10)
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

  private lazy val projectGen: Gen[Project] = for {
    projectId <- projectIdGen
    name <- stringGen(10)
    active <- booleanGen
    repository <- optionGen(repositoryGen)
    visibility <- Gen.oneOf(Visibility.values)
  } yield Project(projectId, UserId.random, name, active, repository, visibility)

  private def optionGen[T](gen: Gen[T]): Gen[Option[T]] = gen.flatMap(Gen.option(_))

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

  lazy val projectAdditionRequestGen: Gen[ProjectAdditionRequest] = stringGen(10).map(ProjectAdditionRequest(_))

  lazy val projectDeleteRequestGen: Gen[ProjectDeleteRequest] = projectIdGen.map(ProjectDeleteRequest(_))

  lazy val projectUpdateRequestGen: Gen[ProjectUpdateRequest] = for {
    id <- projectIdGen
    name <- stringGen(10)
    repository <- optionGen(repositoryGen)
  } yield ProjectUpdateRequest(id, name, repository)

  lazy val projectFileContentGen: Gen[ProjectFileContent] = stringGen(10).map(ProjectFileContent(_))

  lazy val projectUpdateFileRequestGen: Gen[ProjectUpdateFileRequest] = for {
    project <- projectGen
    projectFile <- projectFileGen
    pipelineVersion <- optionGen(pipelineVersionGen)
  } yield ProjectUpdateFileRequest(project, projectFile, pipelineVersion)

  lazy val projectBuildConfigurationRequestGen: Gen[ProjectBuildConfigurationRequest] = for {
    projectId <- projectIdGen
    projectFile <- projectFileGen
  } yield ProjectBuildConfigurationRequest(projectId, projectFile)

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
