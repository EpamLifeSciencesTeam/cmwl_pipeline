package cromwell.pipeline.service.impls

import cats.data.NonEmptyList
import cromwell.pipeline.datastorage.dto.FileParameter
import cromwell.pipeline.womtool.WomToolAPI
import wom.executable.WomBundle

class WomToolTestImpl(
  womBundles: List[WomBundle],
  strings: List[String],
  fileParameters: List[FileParameter]
) extends WomToolAPI {

  override def validate(content: String): Either[NonEmptyList[String], WomBundle] =
    Right(womBundles.head)

  override def stringInputs(content: String): Either[NonEmptyList[String], String] =
    Right(strings.head)

  override def inputsToList(content: String): Either[NonEmptyList[String], List[FileParameter]] =
    Right(fileParameters)

}

object WomToolTestImpl {

  def apply(
    womBundles: List[WomBundle] = Nil,
    strings: List[String] = Nil,
    fileParameters: List[FileParameter] = Nil
  ): WomToolTestImpl =
    new WomToolTestImpl(womBundles, strings, fileParameters)

  def withErrorMessages(errorMessages: List[String]): WomToolAPI =
    new WomToolAPI {

      override def validate(content: String): Either[NonEmptyList[String], WomBundle] =
        getMessages

      override def stringInputs(content: String): Either[NonEmptyList[String], String] =
        getMessages

      override def inputsToList(content: String): Either[NonEmptyList[String], List[FileParameter]] =
        getMessages

      private def getMessages[T]: Either[NonEmptyList[String], T] =
        Left(NonEmptyList.fromList(errorMessages).get)
    }

}
