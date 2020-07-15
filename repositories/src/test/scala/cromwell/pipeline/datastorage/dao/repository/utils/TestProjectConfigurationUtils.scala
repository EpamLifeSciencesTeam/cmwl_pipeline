package cromwell.pipeline.datastorage.dao.repository.utils

import java.nio.file.{ Path, Paths }

import cromwell.pipeline.datastorage.dto._
import org.scalacheck.Gen

object TestProjectConfigurationUtils {
  lazy val typedValueGen: Gen[TypedValue] =
    Gen.oneOf(stringTypedGen, fileTypedGen, intTypedGen, floatTypedGen, booleanTypedGen)

  lazy val fileParameterGen: Gen[FileParameter] =
    for {
      nameField <- stringGen
      valueField <- typedValueGen
    } yield FileParameter(nameField, valueField)

  lazy val projectFileConfigurationGen: Gen[ProjectFileConfiguration] =
    for {
      pathField <- fileGen
      inputsField <- Gen.nonEmptyListOf(fileParameterGen).suchThat(_.length < 4)
    } yield ProjectFileConfiguration(pathField, inputsField)

  lazy val projectConfigurationGen: Gen[ProjectConfiguration] =
    for {
      projectIdField <- projectIDGen
      configurationsField <- Gen.nonEmptyListOf(projectFileConfigurationGen).suchThat(_.length < 4)
    } yield ProjectConfiguration(projectIdField, configurationsField)

  private lazy val stringGen: Gen[String] = Gen.alphaLowerStr.suchThat(str => str.length < 128 && str.nonEmpty)
  private lazy val fileGen: Gen[Path] = stringGen.map(Paths.get(_))
  private lazy val intGen: Gen[Int] = Gen.choose(-1000, 1000)
  private lazy val floatGen: Gen[Float] = Gen.choose(-10.0f, 10.0f)
  private lazy val booleanGen: Gen[Boolean] = Gen.oneOf(true, false)
  private lazy val projectIDGen: Gen[ProjectId] = stringGen.map(ProjectId(_))

  private lazy val stringTypedGen: Gen[StringTyped] =
    Gen.option(stringGen).map(StringTyped)

  private lazy val fileTypedGen: Gen[FileTyped] =
    Gen.option(fileGen).map(FileTyped)

  private lazy val intTypedGen: Gen[IntTyped] =
    Gen.option(intGen).map(IntTyped)

  private lazy val floatTypedGen: Gen[FloatTyped] =
    Gen.option(floatGen).map(FloatTyped)

  private lazy val booleanTypedGen: Gen[BooleanTyped] =
    Gen.option(booleanGen).map(BooleanTyped)
}
