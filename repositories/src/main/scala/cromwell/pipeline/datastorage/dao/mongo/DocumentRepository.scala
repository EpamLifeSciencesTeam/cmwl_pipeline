package cromwell.pipeline.datastorage.dao.mongo

import cromwell.pipeline.datastorage.dao.mongo.DocumentDecoder.DocumentDecoderSyntax
import cromwell.pipeline.datastorage.dao.mongo.DocumentEncoder.DocumentEncoderSyntax
import cromwell.pipeline.datastorage.dao.mongo.DocumentRepository.updateFailedMsg
import org.mongodb.scala.model.Filters._
import org.mongodb.scala.model.ReplaceOptions
import org.mongodb.scala.result.UpdateResult
import org.mongodb.scala.{ Document, MongoCollection }

import scala.concurrent.{ ExecutionContext, Future }

class DocumentRepository(collection: MongoCollection[Document])(implicit ec: ExecutionContext) {

  private[mongo] def checkAcknowledgement(updateResult: UpdateResult): Future[Unit] =
    if (updateResult.wasAcknowledged) Future.unit
    else Future.failed(new IllegalStateException(updateFailedMsg))

  def upsertOne[T: DocumentEncoder](element: T, fieldName: String, value: String): Future[Unit] =
    collection
      .replaceOne(equal(fieldName, value), element.toDocument, ReplaceOptions().upsert(true))
      .toFuture()
      .flatMap(checkAcknowledgement)

  def getByParam[T: DocumentDecoder](field: String, value: String): Future[Seq[T]] =
    collection.find(equal(field, value)).toFuture().map(_.map(_.fromDocument[T]))
}

object DocumentRepository {
  val updateFailedMsg = "Document update failed"
}
